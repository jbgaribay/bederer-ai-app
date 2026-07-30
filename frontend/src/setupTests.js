import "@testing-library/jest-dom";

// jsdom doesn't implement ResizeObserver, which recharts' ResponsiveContainer
// needs to measure its parent element.
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};
