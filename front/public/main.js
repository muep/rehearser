import { noteAdd } from "./db.js";

import { tuneAdd, tuneRm, tunes, tuneById } from "./tune-api.js";
import { instrumentPage } from "./instrument-page.js";
import { instrumentsPage } from "./instruments-page.js";
import { instrumentNewPage } from "./instrument-new-page.js";
import { section, link, button, paragraph, h2, h3 } from "./elements.js";

const whoami = await fetch("api/whoami").then((r) => r.json());
if (whoami["account-id"] === null) {
  window.location.href = "login.html";
} else {
  const userNameSpan = document.getElementById("user-name");
  userNameSpan.textContent = whoami["account-name"];
}

const contentsAfter = (sep, txt) => {
  const idx = txt.indexOf(sep);
  if (idx === -1) {
    return "";
  }

  return txt.substring(idx + 1);
};

const hashPath = (url) =>
  contentsAfter("#", url)
    .split("/")
    .filter((a) => a.length > 0);

const setMainContent = (element) => {
  const main = document.getElementById("main");
  main.innerHTML = "";
  main.appendChild(element);
};

const start = () => {
  setMainContent(paragraph("Start"));
};

const tuneIndex = async () => {
  const page = section();

  const heading = h2("Tunes");
  page.appendChild(heading);

  const tuneList = document.createElement("ul");

  for (const tune of await tunes()) {
    const tuneItem = document.createElement("li");
    tuneItem.appendChild(link(tune.title, `#tune/${tune.id}`));
    tuneList.appendChild(tuneItem);
  }

  page.appendChild(tuneList);

  page.appendChild(link("New tune", "#tune/new"));

  setMainContent(page);
};

const notfound = () => {
  setMainContent(paragraph("Could not find"));
};

const tuneHeading = (title) => {
  const parentLink = link("Tunes", "#tune");

  const nameSpan = document.createElement("span");
  nameSpan.textContent = " / " + title;

  const heading = document.createElement("h2");
  heading.appendChild(parentLink);
  heading.appendChild(nameSpan);

  return heading;
};

const tunePage = async (id) => {
  const tune = await tuneById(id);
  if (tune === undefined) {
    notfound();
    return;
  }

  const page = section();

  page.appendChild(tuneHeading(tune.title));

  page.appendChild(h3("Description"));

  const description = document.createElement("p");
  description.textContent = tune.description;
  page.appendChild(description);

  page.appendChild(h3("Actions"));
  const rmButton = button("Remove tune", async () => {
    await tuneRm(id);
    location.hash = "tune";
  });

  page.appendChild(rmButton);

  setMainContent(page);
};

const tune_new = () => {
  const page = section();

  page.appendChild(tuneHeading("New"));

  const form = document.createElement("form");
  const nameInput = document.createElement("input");
  form.appendChild(nameInput);

  const addButton = button("Add", async () => {
    await tuneAdd(nameInput.value);
    location.hash = "tune";
  });
  form.appendChild(addButton);

  page.appendChild(form);

  setMainContent(page);
};

const navigate = (path) => {
  if (path.length === 0) {
    start();
  } else if (path[0] === "tune") {
    if (path.length === 1) {
      tuneIndex();
    } else if (path.length === 2 && path[1] === "new") {
      tune_new();
    } else if (path.length === 2) {
      const id = Number.parseInt(path[1]);
      if (Number.isInteger(id)) {
        tunePage(id);
      } else {
        notfound();
      }
    } else {
      notfound();
    }
  } else if (path[0] === "instrument") {
    if (path.length === 1) {
      instrumentsPage(setMainContent);
    } else if (path.length === 2 && path[1] === "new") {
      instrumentNewPage(setMainContent);
    } else if (path.length === 2) {
      const id = Number.parseInt(path[1]);
      instrumentPage(setMainContent, id);
    } else {
      notfound();
    }
  } else {
    notfound();
  }
};

const main = () => {
  navigate(hashPath(document.URL));
};

window.onload = main;
window.onhashchange = (ev) => navigate(hashPath(ev.newURL));
