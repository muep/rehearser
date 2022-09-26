import { postJson } from "./misc.js";

export const instruments = async () => (await fetch("api/variant")).json();

export const instrumentAdd = async (title) =>
  (await postJson("api/variant", { title, description: "" })).json();

export const instrumentRm = (id) =>
  fetch(`api/variant/${id}`, {
    method: "DELETE",
  });

export const instrumentById = async (id) => {
  const resp = await fetch(`api/variant/${id}`);
  if (resp.status !== 200) {
    return undefined;
  }

  return resp.json();
};
