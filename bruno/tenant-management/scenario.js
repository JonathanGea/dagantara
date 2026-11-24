const { URL } = require("node:url");

const tenants = "/api/v1/tenants";
const applications = "/api/v1/tenant-applications";
const catalog = "/api/v1/catalog/products";
const storefront = "/api/v1/storefront/products";
const unknownTenant = "00000000-0000-4000-8000-000000000001";
const permissions = ["tenants.read", "tenants.deactivate", "tenants.activate"];

function scenario({ axios, bru, expect, res }) {
  expect(res.getStatus(), "Login Admin Platform").to.equal(200);
  const admin = res.getBody().data;
  expect(admin.passwordChangeRequired).to.equal(false);
  expect(admin.accessToken).to.be.a("string").and.not.empty;

  const base = new URL(bru.getEnvVar("baseUrl"));
  const stamp = bru.getProcessEnv("DAGANTARA_TM_RUN_ID") || Date.now().toString(36);
  expect(stamp).to.match(/^[a-z0-9]{1,20}$/);
  const password = "Dagantara-fixture-" + stamp;

  async function request(method, path, { site = base, token, body, status = 200, error } = {}) {
    const headers = { Host: site.host };
    if (token) headers.Authorization = "Bearer " + token;
    let response;
    try {
      response = await axios({
        method, url: base.origin + site.pathname.replace(/\/$/, "") + path,
        data: body, headers, proxy: false, validateStatus: () => true
      });
    } catch {
      throw new Error(method + " " + site.host + path + " received no HTTP response");
    }
    const label = method + " " + site.host + site.pathname + " " + path;
    expect(response.status, label + " " + (response.data?.errorCode ?? "")).to.equal(status);
    if (status >= 400) {
      if (error) expect(response.data.errorCode, label).to.equal(error);
      expect(response.data).not.to.have.property("data");
    } else if (status !== 204) {
      expect(response.data.success, label).to.equal(true);
    }
    return response.data;
  }

  function platform(method, path, options = {}) {
    return request(method, path, { token: admin.accessToken, ...options });
  }

  function logIn(site, username, secret = password, denied = false) {
    return request("POST", "/api/v1/auth/login", {
      site, body: { username, password: secret }, status: denied ? 401 : 200,
      error: denied ? "INVALID_CREDENTIALS" : undefined
    });
  }

  async function openAccount(site, username, temporaryPassword) {
    if (temporaryPassword) {
      const initial = (await logIn(site, username, temporaryPassword)).data;
      expect(initial.passwordChangeRequired).to.equal(true);
      await request("POST", "/api/v1/auth/password", {
        site, token: initial.accessToken, body: { newPassword: password }, status: 204
      });
    }
    const session = (await logIn(site, username)).data;
    expect(session.passwordChangeRequired).to.equal(false);
    expect(session.accessToken).to.be.a("string").and.not.empty;
    return session.accessToken;
  }

  function address(label, type) {
    const site = new URL(base);
    if (type === "domain") site.hostname = "tm-" + label + "-" + stamp + ".localhost";
    else site.pathname = "/tm-" + label + "-" + stamp;
    return site;
  }

  async function provision(site, name, viaApplication = false, expectedConflict = false) {
    const payload = {
      storeName: name, phoneNumber: "080000000000", ownerUsername: "owner",
      loginUrl: site.href, loginUrlReady: true
    };
    let path = tenants;
    let body = payload;
    if (viaApplication) {
      const application = (await request("POST", applications, { status: 201,
        body: { storeName: name, phoneNumber: payload.phoneNumber, proposedDomain: site.hostname }
      })).data;
      expect(application.status).to.equal("IN_REVIEW");
      path = applications + "/" + application.id + "/provision";
      body = { ownerUsername: payload.ownerUsername, loginUrl: site.href, loginUrlReady: true };
    }
    const response = await platform("POST", path, {
      body, status: expectedConflict ? 409 : 201, error: expectedConflict ? "LOGIN_URL_IN_USE" : undefined
    });
    if (expectedConflict) return;
    const result = response.data;
    expect(result.tenantId).to.match(/^[a-f0-9-]{36}$/);
    expect(result.loginUrl).to.equal(site.href);
    expect(result.temporaryPassword).to.be.a("string").and.not.empty;
    return { id: result.tenantId, site, name, owner: result.ownerUsername,
      temporaryPassword: result.temporaryPassword,
      storeAddress: site.hostname + site.pathname.replace(/\/$/, "") };
  }

  async function operator(label, permissionCodes) {
    const username = "tm-" + label + "-" + stamp;
    const role = (await platform("POST", "/api/v1/roles", {
      status: 201, body: { code: username.toUpperCase(), name: username }
    })).data;
    const delegated = (await platform("PUT", "/api/v1/roles/" + role.id + "/permissions", {
      body: { permissionCodes }
    })).data;
    expect(delegated.permissionCodes).to.have.members(permissionCodes);
    const user = (await platform("POST", "/api/v1/users", {
      status: 201, body: { username, roleId: role.id }
    })).data;
    return openAccount(base, username, user.temporaryPassword);
  }

  async function allTenants(token = admin.accessToken) {
    const records = [];
    let page = 0;
    while (true) {
      const result = await platform("GET", tenants + "?size=100&page=" + page, { token });
      records.push(...result.data);
      if (result.meta.last) return records;
      page += 1;
    }
  }

  function expected(tenant, status) {
    return { id: tenant.id, name: tenant.name, storeAddress: tenant.storeAddress, status };
  }

  async function verify(tenant, status, token) {
    const records = await allTenants(token);
    expect(records.filter(record => record.id === tenant.id)).to.deep.equal([expected(tenant, status)]);
  }

  function actionPath(tenant, action) {
    return tenants + "/" + tenant.id + "/" + action;
  }

  async function transition(tenant, action, token = admin.accessToken) {
    const result = await platform("POST", actionPath(tenant, action), { token });
    expect(result.data).to.deep.equal(expected(tenant, action === "deactivate" ? "INACTIVE" : "ACTIVE"));
  }

  async function publish(tenant, token, productName) {
    const auth = { site: tenant.site, token };
    const category = (await request("POST", "/api/v1/catalog/categories",
      { ...auth, status: 201, body: { name: "Kopi" } })).data;
    const product = (await request("POST", catalog, { ...auth, status: 201, body: {
      name: productName, description: "Kopi untuk verifikasi Tenant Management", priceIdr: 48000,
      categoryIds: [category.id], primaryCategoryId: category.id, productCode: "KOPI", options: []
    } })).data;
    await request("PUT", catalog + "/" + product.id + "/publication",
      { ...auth, body: { status: "PUBLISHED" } });
    return product.id;
  }

  return {
    async listStatuses() {
      const name = "TM AT01 " + stamp;
      const domain = await provision(address("at01", "domain"), name, true);
      const slug = await provision(address("at01", "slug"), name);
      const legacy = { id: "5a0b121c-013b-4e8d-bd14-a42e8f721001", name: "TM Legacy Store",
        storeAddress: "localhost/tm-legacy" };
      const reader = await operator("reader", ["tenants.read"]);
      const records = await allTenants(reader);
      const sameName = [domain, slug].sort((a, b) => a.id.localeCompare(b.id));
      const first = records.findIndex(record => record.id === sameName[0].id);
      expect(first).to.be.at.least(0);
      for (const [index, tenant] of sameName.entries()) {
        const page = first + index;
        const result = await platform("GET", tenants + "?size=1&page=" + page, { token: reader });
        expect(result.meta).to.include({ page, size: 1, totalElements: records.length });
        expect(result.data).to.deep.equal([expected(tenant, "ACTIVE")]);
      }
      expect(records).to.deep.include(expected(legacy, "ACTIVE"));
      await transition(domain, "deactivate");
      await verify(domain, "INACTIVE", reader);
      await verify(slug, "ACTIVE", reader);
      await transition(domain, "reactivate");
    },

    async authorizeActions() {
      const tenant = await provision(address("at02", "domain"), "TM AT02 " + stamp);
      const other = await provision(address("at02-actor", "domain"), "TM AT02 actor " + stamp);
      const tenantToken = await openAccount(other.site, other.owner, other.temporaryPassword);
      const tenantActor = { site: other.site, token: tenantToken, status: 403, error: "FORBIDDEN" };
      const available = (await platform("GET", "/api/v1/roles/permissions")).data;
      expect(available).to.include.members(permissions);
      const provisioning = await operator("provision", ["tenants.provision"]);
      const deactivate = await operator("deactivate", ["tenants.deactivate"]);
      const activate = await operator("activate", ["tenants.activate"]);
      for (const actor of [
        { status: 401 },
        tenantActor,
        { token: provisioning, status: 403 },
        { token: deactivate, status: 403 },
        { token: activate, status: 403 }
      ]) {
        await request("GET", tenants, actor);
        await verify(tenant, "ACTIVE");
      }
      for (const actor of [
        { status: 401 },
        tenantActor,
        { token: provisioning, status: 403 },
        { token: activate, status: 403 }
      ]) {
        await request("POST", actionPath(tenant, "deactivate"), actor);
        await verify(tenant, "ACTIVE");
      }
      await Promise.all([transition(tenant, "deactivate", deactivate), transition(tenant, "deactivate", deactivate)]);
      await transition(tenant, "deactivate", deactivate);
      await verify(tenant, "INACTIVE");
      for (const actor of [
        { status: 401 }, tenantActor, { token: provisioning, status: 403 }, { token: deactivate, status: 403 }
      ]) {
        await request("POST", actionPath(tenant, "reactivate"), actor);
        await verify(tenant, "INACTIVE");
      }
      for (const [action, token] of [["deactivate", deactivate], ["reactivate", activate]]) {
        await platform("POST", actionPath({ id: unknownTenant }, action), { token, status: 404, error: "NOT_FOUND" });
      }
      await platform("POST", actionPath({ id: unknownTenant }, "reactivate"),
        { token: deactivate, status: 403, error: "FORBIDDEN" });
      await transition(tenant, "reactivate", activate);
      await transition(tenant, "reactivate", activate);
      await verify(tenant, "ACTIVE");
    },

    async closePublicCatalog() {
      const shops = [];
      for (const type of ["domain", "slug"]) {
        const tenant = await provision(address("at03", type), "TM AT03 " + type + " " + stamp);
        const token = await openAccount(tenant.site, tenant.owner, tenant.temporaryPassword);
        const name = "Kopi " + type + " " + stamp;
        const id = await publish(tenant, token, name);
        const list = (await request("GET", storefront, { site: tenant.site })).data;
        const detail = (await request("GET", storefront + "/" + id, { site: tenant.site })).data;
        expect(list.map(product => product.id)).to.deep.equal([id]);
        expect(detail).to.include({ id, name });
        shops.push({ tenant, id, list, detail });
      }
      const unmapped = await request("GET", storefront, { site: address("missing", "slug"),
        status: 404, error: "NOT_FOUND" });
      const problem = result => [result.status, result.errorCode, result.detail];
      for (const [index, shop] of shops.entries()) {
        const { tenant, id, list, detail } = shop;
        const other = shops[1 - index];
        await transition(tenant, "deactivate");
        await verify(tenant, "INACTIVE");
        for (const path of [storefront, storefront + "/" + id]) {
          const missing = await request("GET", path, { site: tenant.site, status: 404, error: "NOT_FOUND" });
          expect(problem(missing)).to.deep.equal(problem(unmapped));
          expect(JSON.stringify(missing)).not.to.include(tenant.name).and.not.to.include(detail.name);
        }
        await provision(tenant.site, "Conflicting " + stamp, false, true);
        expect((await allTenants()).filter(record => record.storeAddress === tenant.storeAddress))
          .to.deep.equal([expected(tenant, "INACTIVE")]);
        await verify(other.tenant, "ACTIVE");
        expect((await request("GET", storefront, { site: other.tenant.site })).data).to.deep.equal(other.list);
        expect((await request("GET", storefront + "/" + other.id, { site: other.tenant.site })).data)
          .to.deep.equal(other.detail);
        await transition(tenant, "reactivate");
        await verify(tenant, "ACTIVE");
        expect((await request("GET", storefront, { site: tenant.site })).data).to.deep.equal(list);
        expect((await request("GET", storefront + "/" + id, { site: tenant.site })).data).to.deep.equal(detail);
      }
    },

    async revokeSessions() {
      const shops = [];
      for (const type of ["domain", "slug"]) {
        const tenant = await provision(address("at04", type), "TM AT04 " + type + " " + stamp);
        const site = tenant.site;
        const owner = await openAccount(site, tenant.owner, tenant.temporaryPassword);
        const auth = { site, token: owner };
        const role = (await request("POST", "/api/v1/tenant/roles", {
          ...auth, status: 201, body: { code: "READER", name: "Catalog reader" }
        })).data;
        await request("PUT", "/api/v1/tenant/roles/" + role.id + "/permissions", {
          ...auth, body: { permissions: ["catalog.read"] }
        });
        async function staff(username) {
          const user = (await request("POST", "/api/v1/tenant/users", {
            ...auth, status: 201, body: { username, roleId: role.id }
          })).data;
          expect(user.account).to.include({ tenantId: tenant.id, username, roleId: role.id, status: "ACTIVE" });
          return { id: user.account.id, token: await openAccount(site, username, user.temporaryPassword) };
        }
        const active = await staff("staff");
        const inactive = await staff("inactive");
        const pending = (await request("POST", "/api/v1/tenant/users", {
          ...auth, status: 201, body: { username: "pending", roleId: role.id }
        })).data;
        const pendingSession = (await logIn(site, "pending", pending.temporaryPassword)).data;
        expect(pendingSession.passwordChangeRequired).to.equal(true);
        const deactivated = (await request("POST", "/api/v1/tenant/users/" + inactive.id + "/deactivate", auth)).data;
        expect(deactivated).to.include({ id: inactive.id, status: "INACTIVE" });
        await logIn(site, "inactive", password, true);
        const id = await publish(tenant, owner, "Kopi internal " + type + " " + stamp);
        const resource = catalog + "/" + id;
        const product = (await request("GET", resource, auth)).data;
        expect((await request("GET", resource, { site, token: active.token })).data).to.deep.equal(product);
        shops.push({ tenant, site, owner, active, pending, pendingSession, resource, product });
      }
      for (const [index, shop] of shops.entries()) {
        const { tenant, site, owner, active, pending, pendingSession, resource, product } = shop;
        const other = shops[1 - index];
        await transition(tenant, "deactivate");
        for (const username of [tenant.owner, "staff"]) await logIn(site, username, password, true);
        await logIn(site, "pending", pending.temporaryPassword, true);
        for (const token of [owner, active.token]) await request("GET", resource, { site, token, status: 401 });
        expect((await request("GET", other.resource, { site: other.site, token: other.owner })).data)
          .to.deep.equal(other.product);
        await transition(tenant, "reactivate");
        for (const token of [owner, active.token]) await request("GET", resource, { site, token, status: 401 });
        await request("POST", "/api/v1/auth/password", {
          site, token: pendingSession.accessToken, body: { newPassword: password }, status: 401
        });
        for (const username of [tenant.owner, "staff"]) {
          const token = await openAccount(site, username);
          expect((await request("GET", resource, { site, token })).data).to.deep.equal(product);
          if (username === tenant.owner) shop.owner = token;
        }
        await logIn(site, "inactive", password, true);
        const freshPending = (await logIn(site, "pending", pending.temporaryPassword)).data;
        expect(freshPending.passwordChangeRequired).to.equal(true);
      }
    }
  };
}

module.exports = { scenario };
