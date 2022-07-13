export const tunes = async () => (await fetch("api/exercise")).json();

const postJson = (url, body) =>
  fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });

export const tuneAdd = async (title) =>
  (await postJson("api/exercise", { title, description: "" })).json();

export const tuneRm = (id) =>
  fetch(`api/exercise/${id}`, {
    method: "DELETE",
  });

export const tuneById = async (id) => {
  const resp = await fetch(`api/exercise/${id}`);
  if (resp.status !== 200) {
    return undefined;
  }

  return resp.json();
};
