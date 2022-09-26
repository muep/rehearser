import { button, objectHeading, section } from "./elements.js";
import { instrumentAdd } from "./instrument-api.js";

export const instrumentNewPage = (setMainContent) => {
  const page = section();
  page.append(objectHeading("Instrument", "#instrument", "New"));

  const form = document.createElement("form");
  const nameInput = document.createElement("input");
  form.appendChild(nameInput);

  const addButton = button("Add", async () => {
    await instrumentAdd(nameInput.value);
    location.hash = "instrument";
  });
  form.appendChild(addButton);

  page.appendChild(form);
  setMainContent(page);
};
