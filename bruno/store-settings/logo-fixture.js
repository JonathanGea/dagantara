const { Buffer } = require("buffer");
const { crc32 } = require("zlib");

const png = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAm0lEQVR4nO3asQmEAAAEwXX5UizAJuzA0FY++nYsxpJsQXhQFp34goNLbxjXmTKJkziJkziJkziJkziJkziJkziJ+5yP7r+FC03f7RELSJzESZzESZzESZzESZzESZzESZzESZzESZzESZzESZzESZzESZzESZzESZzESZzESZzESdzwXs5uJnESJ3ESJ3ESJ3ESJ3ES590F/nUAMNIFCx1rIpkAAAAASUVORK5CYII=", "base64");
const webp = Buffer.from("UklGRnwAAABXRUJQVlA4THAAAAAvP8APAA/wMOjnxR8Sd/7jAY5j22qbq7ykBJVCaag0lUIJWmYTHs72PqL/ExD+qfGlioKDUyysYsDLALM0MEkFPAZwC8HJ+YiV9RWjLL/AyvoKJ+cjQW4hhAoIDzUwyQCzGPBiYRUHp4SKUDS+9OMB", "base64");
const aFields = { displayName: "Kopi Pagi", publicWhatsapp: "+6281234567890", publicEmail: "kopi@example.test" };
const bFields = { displayName: "Teh Sore", publicWhatsapp: "+6281234567891", publicEmail: "teh@example.test" };
const logoPath = "/api/v1/storefront/logo/";

function oversizePng() {
  // Add a valid ancillary text chunk before IEND so the fixture is an image over 2 MiB.
  const text = Buffer.alloc(2 * 1024 * 1024, "x");
  text.write("Description\0");
  const type = Buffer.from("tEXt");
  const length = Buffer.alloc(4);
  length.writeUInt32BE(text.length);
  const checksum = Buffer.alloc(4);
  checksum.writeUInt32BE(crc32(text, crc32(type)));
  return Buffer.concat([png.subarray(0, -12), length, type, text, checksum, png.subarray(-12)]);
}

async function replace(fixture, store, token, fields, bytes) {
  return fixture.save(store, token, { ...fields, logoAction: "REPLACE" }, { logos: [bytes] });
}

async function checkImage(fixture, expect, store, url, type, bytes) {
  const image = await fixture.request(new URL(store).origin, "GET", url, { binary: true });
  expect(image.headers["content-type"]).to.equal(type);
  expect(image.headers["cache-control"]).to.include("no-store");
  expect(image.bytes.equals(bytes), "Image bytes match the upload").to.equal(true);
}

async function missing(fixture, store, url) {
  await fixture.request(new URL(store).origin, "GET", url, { status: 404, error: "NOT_FOUND" });
}

module.exports = { png, webp, aFields, bFields, logoPath, oversizePng, replace, checkImage, missing };
