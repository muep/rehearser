import {
  instrumentAdd,
  instrumentRm,
  instruments,
  instrumentById,
} from "./instrument-api.js";

import { link, objectHeading, section, span } from "./elements.js";

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
