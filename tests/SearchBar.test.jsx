import { fireEvent, render, screen } from "@testing-library/react";
import Header from "../components/Header";

describe("SearchBar in Header", () => {
  test("calls setSearchTerm when user types", () => {
    const setSearchTerm = jest.fn();

    render(
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
    fireEvent.change(input, { target: { value: "batman" } });

    expect(setSearchTerm).toHaveBeenCalledWith("batman");
  });
});
