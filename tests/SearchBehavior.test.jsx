import { fireEvent, render, screen } from "@testing-library/react";
import Header from "../components/Header";

describe("Search input behavior", () => {
  test("search input reflects typed value in controlled mode", () => {
    const setSearchTerm = jest.fn();

    const { rerender } = render(
      <Header
        searchTerm=""
        setSearchTerm={setSearchTerm}
        sortYear=""
        setSortYear={jest.fn()}
        darkMode={false}
        setDarkMode={jest.fn()}
      />
    );

    const input = screen.getByPlaceholderText("Search movies...");
    fireEvent.change(input, { target: { value: "joker" } });
    expect(setSearchTerm).toHaveBeenCalledWith("joker");

    rerender(
      <Header
        searchTerm="joker"
        setSearchTerm={setSearchTerm}
        sortYear=""
        setSortYear={jest.fn()}
        darkMode={false}
        setDarkMode={jest.fn()}
      />
    );

    expect(screen.getByDisplayValue("joker")).toBeInTheDocument();
  });
});
