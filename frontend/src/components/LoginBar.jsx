import React, { useState } from "react";
import { useAuth } from "../context/AuthContext.jsx";

export default function LoginBar() {
  const { isAuthenticated, login, logout } = useAuth();
  const [username, setUsername] = useState("demo");
  const [password, setPassword] = useState("");

  if (isAuthenticated) {
    return (
      <div className="flex items-center justify-between bg-white rounded-lg shadow px-4 py-2 border-2 border-green-200 mb-4">
        <span className="text-sm text-gray-700">Signed in as {username}</span>
        <button
          type="button"
          onClick={logout}
          className="text-sm font-semibold text-red-500 hover:text-red-700"
        >
          Sign out
        </button>
      </div>
    );
  }

  return (
    <form
      className="flex flex-wrap items-center gap-2 bg-white rounded-lg shadow px-4 py-3 border-2 border-green-200 mb-4"
      onSubmit={(e) => {
        e.preventDefault();
        login(username, password);
      }}
    >
      <span className="text-sm text-gray-600 mr-2">Sign in to analyze a swing:</span>
      <input
        aria-label="username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder="Username"
        className="border rounded px-2 py-1 text-sm"
      />
      <input
        aria-label="password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="Password"
        className="border rounded px-2 py-1 text-sm"
      />
      <button
        type="submit"
        className="bg-green-600 text-white text-sm font-semibold px-3 py-1 rounded hover:bg-green-700"
      >
        Sign in
      </button>
    </form>
  );
}
