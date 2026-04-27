import { fireEvent, render, screen } from "@testing-library/react";
import MovieCard from "../components/MovieCard";

describe("MovieCard", () => {
  test("renders title and rating", () => {
    const movie = {
      id: 1,
      title: "Inception",
      rating: "8.8",
      poster: "https://example.com/poster.jpg",
    };

    render(<MovieCard movie={movie} openModal={jest.fn()} />);

    expect(screen.getByText("Inception")).toBeInTheDocument();
    expect(screen.getByText("8.8")).toBeInTheDocument();
  });

  test("calls openModal with selected movie on click", () => {
    const movie = {
      id: 2,
      title: "Interstellar",
      rating: "8.6",
      poster: "https://example.com/interstellar.jpg",
    };
    const openModal = jest.fn();

    render(<MovieCard movie={movie} openModal={openModal} />);

    fireEvent.click(screen.getByText("Interstellar"));
    expect(openModal).toHaveBeenCalledWith(movie);
  });
});
