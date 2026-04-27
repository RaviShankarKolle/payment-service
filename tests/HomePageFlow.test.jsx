import { fireEvent, render, screen } from "@testing-library/react";
import HomePage from "../pages/HomePage";

const movies = [
  { id: 1, title: "Inception", rating: "8.8", poster: "https://example.com/a.jpg" },
  { id: 2, title: "Interstellar", rating: "8.6", poster: "https://example.com/b.jpg" },
];

describe("HomePage result rendering and movie detail navigation", () => {
  test("renders movie results", () => {
    render(
      <HomePage
        movies={movies}
        openMovieDetails={jest.fn()}
        canLoadMore={false}
        onLoadMore={jest.fn()}
      />
    );

    expect(screen.getByText("Inception")).toBeInTheDocument();
    expect(screen.getByText("Interstellar")).toBeInTheDocument();
  });

  test("navigates to movie detail flow by calling openMovieDetails", () => {
    const openMovieDetails = jest.fn();

    render(
      <HomePage
        movies={movies}
        openMovieDetails={openMovieDetails}
        canLoadMore={false}
        onLoadMore={jest.fn()}
      />
    );

    fireEvent.click(screen.getByText("Inception"));
    expect(openMovieDetails).toHaveBeenCalledWith(movies[0]);
  });
});
