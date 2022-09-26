export const button = (text, onclick) => {
  const b = document.createElement("button");
  b.setAttribute("type", "button");
  b.textContent = text;
  b.onclick = onclick;
  return b;
};

export const h2 = (text) => {
  const h = document.createElement("h2");
  h.textContent = text;
  return h;
};

export const h3 = (text) => {
  const h = document.createElement("h3");
  h.textContent = text;
  return h;
};

export const link = (text, dest) => {
  const a = document.createElement("a");
  a.setAttribute("href", dest);
  a.textContent = text;
  return a;
};

export const objectHeading = (groupName, groupDest, title) => {
  const parentLink = link(groupName, groupDest);

  const nameSpan = span();
  nameSpan.textContent = " / " + title;

  const heading = document.createElement("h2");
  heading.appendChild(parentLink);
  heading.appendChild(nameSpan);

  return heading;
};

export const paragraph = (text) => {
  const p = document.createElement("p");
  p.textContent = text;
  return p;
};

export const section = () => document.createElement("section");
export const span = () => document.createElement("span");
