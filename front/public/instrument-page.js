import { instrumentById } from "./instrument-api.js";

import { objectHeading, section } from "./elements.js";

const instrumentHeading = (title) =>
  objectHeading("Instrument", "#instrument", title);

export const instrumentPage = async (setMainContent, id) => {
  const instrument = await instrumentById(id);
  if (instrument === undefined) {
    return undefined;
  }

  const page = section();

  page.appendChild(instrumentHeading(instrument.title));

  setMainContent(page);
};
