const maxId = (prefix) => {
  const id = Number.parseInt(localStorage.getItem(`${prefix}/maxid`));
  if (!Number.isInteger(id)) {
    return 0;
  }

  return id;
};

const nextId = (prefix) => maxId(prefix) + 1;

const notesOfTune = (tuneId) => {
  const top = maxId(`tune/${tuneId}/note`);
  const res = [];

  for (let id = 1; id <= top; ++id) {
    const text = localStorage.getItem(`tune/${tuneId}/note/${id}`);
    if (text === null) {
      continue;
    }

    res.push({ id, text });
  }

  return res;
};

export const tunes = () => {
  const top = maxId("tune");

  const res = [];

  for (let id = 1; id <= top; ++id) {
    const name = localStorage.getItem(`tune/${id}/name`);
    if (name === null) {
      continue;
    }

    res.push({ id, name });
  }

  return res;
};

export const tuneById = (id) => {
  const name = localStorage.getItem(`tune/${id}/name`);
  if (name === null) {
    return null;
  }

  const notes = notesOfTune(id);

  return { id, name, notes };
};

export const tuneAdd = (name) => {
  const id = nextId("tune");
  localStorage.setItem("tune/maxid", `${id}`);
  localStorage.setItem(`tune/${id}/name`, name);
};

export const tuneRm = (id) => {
  for (const n of notesOfTune(id)) {
    localStorage.removeItem(`tune/${id}/note/${n.id}`);
  }
  localStorage.removeItem(`tune/${id}/note/maxid`);
  localStorage.removeItem(`tune/${id}/name`);
};

export const noteAdd = (tuneId, note) => {
  const id = nextId(`tune/${tuneId}/note`);
  localStorage.setItem(`tune/${tuneId}/note/maxid`, `${id}`);
  localStorage.setItem(`tune/${tuneId}/note/${id}`, note);
};
