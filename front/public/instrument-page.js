import { instrumentById, instrumentRm } from "./instrument-api.js";

import { button, h3, objectHeading, section } from "./elements.js";

const instrumentHeading = (title) =>
  objectHeading("Instrument", "#instrument", title);

export const instrumentPage = async (setMainContent, id) => {
  const instrument = await instrumentById(id);
  if (instrument === undefined) {
    return undefined;
  }

  const page = section();

  page.appendChild(instrumentHeading(instrument.title));

  page.appendChild(h3("Actions"));
  page.appendChild(
    button("Remove instrument", async () => {
      await instrumentRm(id);
      location.hash = "instrument";
    })
  );

  setMainContent(page);
};
