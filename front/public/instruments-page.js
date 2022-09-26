import {
  instrumentAdd,
  instrumentRm,
  instruments,
  instrumentById,
} from "./instrument-api.js";

import { h2, link, objectHeading, section, span } from "./elements.js";

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

  setMainContent(page);
};
