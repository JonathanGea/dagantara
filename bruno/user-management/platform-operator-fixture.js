const axios = require("axios");

function platformFixture(bru, expect, res) {
  expect(res.getStatus(), "bootstrap fixture login").to.equal(200);
  const admin = res.getBody().data;
  expect(admin.passwordChangeRequired).to.equal(false);
  expect(isBearer(admin.accessToken), "admin bearer token").to.equal(true);

  const baseUrl = bru.getEnvVar("baseUrl");
  const suffix = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
  const adminToken = admin.accessToken;

  async function request(method, path, { token = adminToken, body, status = 200, error, label = path } = {}) {
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    let response;
    try {
      response = await axios({ method, url: `${baseUrl}${path}`, data: body, headers });
    } catch (failure) {
      response = failure.response;
      if (!response) throw new Error(`${method} ${label} received no HTTP response`);
    }
    expect(response.status, `${method} ${label}`).to.equal(status);
    if (error) expect(response.data.errorCode, `${method} ${label} error code`).to.equal(error);
    return response;
  }

  function api(method, path, options) {
    return request(method, `/api/v1${path}`, options);
  }

  async function role(label, permissionCodes = []) {
    const code = name(label);
    const created = await api("POST", "/roles", { status: 201, body: { code, name: code } });
    const data = created.data.data;
    expect(data.code).to.equal(code);
    expect(data.permissionCodes).to.deep.equal([]);
    expect(isUuid(data.id), "created role UUID").to.equal(true);
    if (permissionCodes.length) {
      const update = await api("PUT", `/roles/${data.id}/permissions`, { body: { permissionCodes } });
      expect(update.data.data.permissionCodes).to.have.members(permissionCodes);
    }
    return data.id;
  }

  async function create(username, roleId, token = adminToken) {
    const response = await api("POST", "/users", { token, status: 201, body: { username, roleId } });
    const credentials = response.data.data;
    expect(response.headers["cache-control"]).to.include("no-store");
    expect(credentials.username).to.equal(username);
    expect(credentials.roleId).to.equal(roleId);
    expect(isUuid(credentials.id), "created user UUID").to.equal(true);
    expect(isTemporaryPassword(credentials.temporaryPassword), "generated temporary password").to.equal(true);
    return credentials;
  }

  async function login(username, password, temporary = false) {
    const response = await api("POST", "/auth/login", { token: null, body: { username, password } });
    const data = response.data.data;
    expect(isBearer(data.accessToken), "operator bearer token").to.equal(true);
    expect(data.passwordChangeRequired).to.equal(temporary);
    return data.accessToken;
  }

  async function activate(credentials) {
    const temporaryToken = await login(credentials.username, credentials.temporaryPassword, true);
    const password = name("new-password");
    await api("POST", "/auth/password", { token: temporaryToken, status: 204, body: { newPassword: password } });
    return { ...credentials, password, token: await login(credentials.username, password), temporaryToken };
  }

  async function operator(label, roleId) {
    return activate(await create(name(label), roleId));
  }

  async function users() {
    const first = await api("GET", "/users?size=100");
    const items = [...first.data.data];
    for (let page = 1; page < first.data.meta.totalPages; page++) {
      const next = await api("GET", `/users?size=100&page=${page}`);
      items.push(...next.data.data);
    }
    expect(items.length, "platform operator list total").to.equal(first.data.meta.totalElements);
    return items;
  }

  async function detail(id, token = adminToken) {
    return (await api("GET", `/users/${id}`, { token })).data.data;
  }

  async function roles() {
    return (await api("GET", "/roles")).data.data;
  }

  function name(label) {
    return `um03-${label}-${suffix}`;
  }

  async function rejectLogin(username, password) {
    await api("POST", "/auth/login", {
      token: null, status: 401, error: "INVALID_CREDENTIALS", body: { username, password }, label: "rejected login"
    });
  }

  return { api, request, role, create, login, activate, operator, users, detail, roles, name, rejectLogin };
}

function isUuid(value) {
  return /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(value);
}

function isBearer(value) {
  return /^[^.]+\.[^.]+\.[^.]+$/.test(value);
}

function isTemporaryPassword(value) {
  return /^[\w-]{32}$/.test(value);
}

module.exports = { platformFixture, isTemporaryPassword };
