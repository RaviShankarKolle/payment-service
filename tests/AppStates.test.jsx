import { render, screen } from "@testing-library/react";
import App from "../App";
import useMovies from "../hooks/useMovies";

jest.mock("../hooks/useMovies", () => jest.fn());
jest.mock("../components/Header", () => () => <div>Header</div>);
jest.mock("../pages/HomePage", () => () => <div>Home Page</div>);
jest.mock("../pages/SearchPage", () => () => <div>Search Page</div>);
jest.mock("../pages/MovieDetailsPage", () => () => <div>Movie Details Page</div>);

const defaultHookState = {
  modalMovie: null,
  setModalMovie: jest.fn(),
  searchTerm: "",
  setSearchTerm: jest.fn(),
  sortYear: "",
  setSortYear: jest.fn(),
  darkMode: false,
  setDarkMode: jest.fn(),
  error: "",
  loading: false,
  apiKeyMissing: false,
  displayMovies: [],
  isSearchScreen: false,
  canLoadMore: false,
  handleLoadMore: jest.fn(),
};

describe("App loading and error states", () => {
  test("shows loading state message", () => {
    useMovies.mockReturnValue({
      ...defaultHookState,
      loading: true,
    });

    render(<App />);
    expect(screen.getByText("Loading movies...")).toBeInTheDocument();
  });

  test("shows error state message", () => {
    useMovies.mockReturnValue({
      ...defaultHookState,
      error: "Failed to fetch movies from TMDB.",
    });

    render(<App />);
    expect(screen.getByText("Failed to fetch movies from TMDB.")).toBeInTheDocument();
  });
});
