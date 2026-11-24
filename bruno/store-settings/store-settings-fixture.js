const axios = require("axios");

const settingsPath = "/api/v1/store-settings";
const catalogPath = "/api/v1/catalog";
const publicProducts = "/api/v1/storefront/products";
const blank = { displayName: null, publicWhatsapp: null, publicEmail: null };

async function fixture(bru, expect, res) {
  const base = bru.getEnvVar("baseUrl");
  const platform = base;
  const a = base.replace("localhost", "127.0.0.1");
  const b = base + "/ss01-b";
  const names = { [a]: "Kopi Pagi Nusantara", [b]: "Teh Sore Nusantara" };
  const password = bru.getProcessEnv("SS01_FIXTURE_PASSWORD");
  const suffix = Date.now().toString(36);
  expect(res.getStatus(), "Tenant B owner login").to.equal(200);
  const ownerB = tokenFrom(res.getBody(), false);

  function tokenFrom(body, temporary) {
    expect(body.data.passwordChangeRequired).to.equal(temporary);
    expect(Boolean(body.data.accessToken), "bearer token is present").to.equal(true);
    return body.data.accessToken;
  }

  async function request(store, method, path, { token, body, status = 200, error, contentType } = {}) {
    const headers = {};
    if (token) headers.Authorization = "Bearer " + token;
    if (contentType) headers["Content-Type"] = contentType;
    let response;
    try {
      response = await axios({ url: store + path, method, data: body, headers, validateStatus: () => true });
    } catch {
      throw new Error(method + " " + path + " received no HTTP response");
    }
    expect(response.status, method + " " + path + " " + (response.data?.errorCode ?? "")).to.equal(status);
    if (status >= 400) {
      expect(response.headers["content-type"]).to.include("application/problem+json");
      expect(response.data.status).to.equal(status);
      if (error) expect(response.data.errorCode).to.equal(error);
    } else if (response.headers["content-type"]?.includes("application/json")) {
      expect(response.data.success).to.equal(true);
    }
    return response.data;
  }

  async function login(store, username = "ss01-owner", enteredPassword = password, temporary = false) {
    const result = await request(store, "POST", "/api/v1/auth/login", { body: { username, password: enteredPassword } });
    return tokenFrom(result, temporary);
  }

  async function read(store, token, options) {
    return (await request(store, "GET", settingsPath, { token, ...options })).data;
  }

  async function save(store, token, fields, options) {
    const boundary = "dagantara-ss01-" + suffix;
    const payload = JSON.stringify({ logoAction: "KEEP", ...fields });
    const body = [
      "--" + boundary, 'Content-Disposition: form-data; name="settings"', "Content-Type: application/json",
      "", payload, "--" + boundary + "--", ""
    ].join("\r\n");
    const result = await request(store, "PUT", settingsPath, {
      token, body, contentType: "multipart/form-data; boundary=" + boundary, ...options
    });
    return options?.status >= 400 ? result : result.data;
  }

  function checkSettings(store, result, fields) {
    expect(result).to.deep.equal({ tenantName: names[store], ...fields, logoUrl: null });
  }

  async function page(store, fields, productId) {
    const html = await request(store, "GET", "/products" + (productId ? "/" + productId : ""));
    const embedded = html.match(/<script type="application\/json" id="product-data">(.+)<\/script>/);
    expect(Boolean(embedded), "product and store JSON in page").to.equal(true);
    const { store: identity, catalog } = JSON.parse(embedded[1]);
    const { displayName, ...contact } = fields;
    expect(identity).to.deep.equal({ name: displayName ?? names[store], ...contact });
    return catalog.data;
  }

  async function publish(store, token, label) {
    const category = (await request(store, "POST", catalogPath + "/categories", {
      token, status: 201, body: { name: "SS01 " + label + " " + suffix }
    })).data;
    const product = (await request(store, "POST", catalogPath + "/products", {
      token, status: 201, body: {
        name: label, priceIdr: 25000, productCode: "SS01" + suffix,
        categoryIds: [category.id], primaryCategoryId: category.id
      }
    })).data;
    expect(product.status).to.equal("DRAFT");
    await publication(store, token, product, "PUBLISHED");
    return product;
  }

  async function publication(store, token, product, status) {
    if (!product) return;
    const updated = await request(store, "PUT", catalogPath + "/products/" + product.id + "/publication",
      { token, body: { status } });
    expect(updated.data.status).to.equal(status);
  }

  async function pages(store, fields, product) {
    const cards = await page(store, fields);
    expect(cards.map(card => card.id)).to.include(product.id);
    const detail = await page(store, fields, product.id);
    expect(detail).to.include({ id: product.id, name: product.name });
  }

  const ownerA = await login(a);
  return { a, b, platform, ownerA, ownerB, suffix, request, login, read, save, checkSettings, page, pages,
    publish, publication, publicProducts, blank };
}

module.exports = { fixture };
