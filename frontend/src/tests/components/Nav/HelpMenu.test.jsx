import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import HelpMenu from "main/components/Nav/HelpMenu";

describe("HelpMenu tests", () => {
  test("About link points to the /about route", async () => {
    render(
      <MemoryRouter>
        <HelpMenu />
      </MemoryRouter>,
    );
    fireEvent.click(screen.getByText("Help"));
    const aboutLink = await screen.findByText("About");
    expect(aboutLink).toHaveAttribute("href", "/about");
  });
});
