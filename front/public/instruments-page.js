import { instruments } from "./instrument-api.js";

import { h2, link, section } from "./elements.js";

export const instrumentsPage = async (setMainContent) => {
  const page = section();
  page.appendChild(h2("Instruments"));

  const instList = document.createElement("ul");

  for (const { id, title } of await instruments()) {
    const instItem = document.createElement("li");
    instItem.appendChild(link(title, `#instrument/${id}`));
    instList.appendChild(instItem);
  }

  page.appendChild(instList);

  page.appendChild(link("New instrument", "#instrument/new"));

  setMainContent(page);
};
