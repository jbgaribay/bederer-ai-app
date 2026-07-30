import React from "react";
import { render, screen } from "@testing-library/react";
import CoachingReport from "./CoachingReport.jsx";

const sampleAnalysis = {
  shotType: "forehand",
  overallScore: 8.2,
  topPriority: "Extend your follow-through",
  drillRecommendation: "Shadow swings, 3 sets of 10",
  categories: [
    {
      name: "Contact Point",
      score: 9,
      severity: "GOOD",
      observation: "Excellent contact in front of the body",
      tip: "Try making contact 2 inches earlier",
      frameIndex: 0,
    },
  ],
};

describe("CoachingReport", () => {
  test("renders overall score and shot type", () => {
    render(<CoachingReport analysis={sampleAnalysis} />);
    expect(screen.getByText("8.2")).toBeInTheDocument();
    expect(screen.getByText(/forehand analysis/i)).toBeInTheDocument();
  });

  test("renders each category with its observation and tip", () => {
    render(<CoachingReport analysis={sampleAnalysis} />);
    expect(screen.getByText("Contact Point")).toBeInTheDocument();
    expect(screen.getByText("Excellent contact in front of the body")).toBeInTheDocument();
    expect(screen.getByText("Try making contact 2 inches earlier")).toBeInTheDocument();
  });

  test("renders top priority and drill recommendation", () => {
    render(<CoachingReport analysis={sampleAnalysis} />);
    expect(screen.getByText("Extend your follow-through")).toBeInTheDocument();
    expect(screen.getByText("Shadow swings, 3 sets of 10")).toBeInTheDocument();
  });
});
