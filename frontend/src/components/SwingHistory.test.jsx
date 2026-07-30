import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import SwingHistory from "./SwingHistory.jsx";

const sampleHistory = [
  {
    id: 1,
    shotType: "forehand",
    overallScore: 7.6,
    topPriority: "Extend your follow-through",
    createdAt: "2026-07-29T12:00:00Z",
    categories: [
      { name: "Stance & Preparation", score: 8, severity: "GOOD" },
    ],
  },
];

describe("SwingHistory", () => {
  test("renders empty state with no swings", () => {
    render(<SwingHistory history={[]} onDelete={() => {}} canEdit={false} />);
    expect(screen.getByText(/no swings yet/i)).toBeInTheDocument();
  });

  test("renders swing summary details", () => {
    render(<SwingHistory history={sampleHistory} onDelete={() => {}} canEdit={false} />);
    expect(screen.getByText("7.6")).toBeInTheDocument();
    expect(screen.getByText("forehand")).toBeInTheDocument();
  });

  test("expands to show category detail on click", () => {
    render(<SwingHistory history={sampleHistory} onDelete={() => {}} canEdit={false} />);
    fireEvent.click(screen.getByText("forehand"));
    expect(screen.getByText("Extend your follow-through")).toBeInTheDocument();
  });

  test("calls onDelete with swing id when delete is clicked", () => {
    const handleDelete = jest.fn();
    render(<SwingHistory history={sampleHistory} onDelete={handleDelete} canEdit={true} />);
    fireEvent.click(screen.getByRole("button", { name: /delete/i }));
    expect(handleDelete).toHaveBeenCalledWith(1);
  });

  test("hides delete button when not authenticated", () => {
    render(<SwingHistory history={sampleHistory} onDelete={() => {}} canEdit={false} />);
    expect(screen.queryByRole("button", { name: /delete/i })).not.toBeInTheDocument();
  });
});
