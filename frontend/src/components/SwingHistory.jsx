import React, { useState } from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
} from "recharts";

// Ported from the original SwingHistory.tsx. The original read from
// localStorage-backed SavedSwing[] (with a `date` string and `timestamp`);
// this reads straight from the backend's SwingAnalysisResponse[] (with
// `createdAt` as an ISO instant and a real numeric `id`), and deletion now
// hits DELETE /api/swings/{id} per-card instead of a single "clear all"
// localStorage wipe, since that's what the REST API actually exposes.

function formatDate(isoString) {
  return new Date(isoString).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

function getScoreColor(score) {
  if (score >= 8) return "text-green-600";
  if (score >= 6) return "text-yellow-600";
  return "text-red-600";
}

function getScoreBadgeColor(score) {
  if (score >= 8) return "bg-green-100 text-green-800 border-green-300";
  if (score >= 6) return "bg-yellow-100 text-yellow-800 border-yellow-300";
  return "bg-red-100 text-red-800 border-red-300";
}

function getSeverityColor(severity) {
  switch (severity) {
    case "GOOD": return "text-green-600";
    case "NEEDS_WORK": return "text-yellow-600";
    case "CRITICAL": return "text-red-600";
    default: return "text-gray-600";
  }
}

export default function SwingHistory({ history, onDelete, canEdit }) {
  const [expandedId, setExpandedId] = useState(null);

  const chartData = [...history]
    .reverse()
    .map((swing, index) => ({
      name: `#${index + 1}`,
      score: parseFloat(swing.overallScore.toFixed(1)),
      date: formatDate(swing.createdAt),
      shot: swing.shotType,
    }));

  if (history.length === 0) {
    return (
      <div className="bg-white rounded-xl shadow-2xl p-12 border-4 border-dashed border-green-300 text-center">
        <div className="text-6xl mb-4">📈</div>
        <h3 className="text-2xl font-bold text-gray-900 mb-2">No Swings Yet</h3>
        <p className="text-gray-600">Analyze your first swing and it'll appear here automatically.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="bg-white rounded-xl shadow-2xl p-6 border-4 border-green-600">
        <h2 className="text-2xl font-black text-gray-900 mb-1">My Progress</h2>
        <p className="text-gray-500 text-sm">
          {history.length} swing{history.length !== 1 ? "s" : ""} recorded · Last 10 saved
        </p>
      </div>

      <div className="bg-white rounded-xl shadow-2xl p-6 border-4 border-green-600">
        <h3 className="text-lg font-black text-gray-900 mb-6">Overall Score Over Time</h3>
        <ResponsiveContainer width="100%" height={220}>
          <LineChart data={chartData} margin={{ top: 5, right: 20, left: -10, bottom: 5 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
            <XAxis dataKey="name" tick={{ fontSize: 12, fontWeight: 700 }} stroke="#6b7280" />
            <YAxis domain={[0, 10]} ticks={[0, 2, 4, 6, 8, 10]} tick={{ fontSize: 12, fontWeight: 700 }} stroke="#6b7280" />
            <Tooltip
              content={({ active, payload }) => {
                if (active && payload && payload.length) {
                  const d = payload[0].payload;
                  return (
                    <div className="bg-white border-2 border-green-600 rounded-lg p-3 shadow-lg">
                      <p className="font-black text-green-700 text-lg">{d.score}/10</p>
                      <p className="text-gray-600 text-sm capitalize">{d.shot}</p>
                      <p className="text-gray-400 text-xs">{d.date}</p>
                    </div>
                  );
                }
                return null;
              }}
            />
            <ReferenceLine y={8} stroke="#16a34a" strokeDasharray="4 4" label={{ value: "Great", position: "right", fontSize: 11, fill: "#16a34a" }} />
            <ReferenceLine y={6} stroke="#ca8a04" strokeDasharray="4 4" label={{ value: "Good", position: "right", fontSize: 11, fill: "#ca8a04" }} />
            <Line
              type="monotone"
              dataKey="score"
              stroke="#16a34a"
              strokeWidth={3}
              dot={{ fill: "#16a34a", strokeWidth: 2, r: 5 }}
              activeDot={{ r: 7, fill: "#15803d" }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <div className="space-y-4">
        <h3 className="text-lg font-black text-gray-900 px-1">Recent Swings</h3>
        {history.map((swing) => (
          <div key={swing.id} className="bg-white rounded-xl shadow-lg border-4 border-green-200 hover:border-green-500 transition-all">
            <div className="w-full p-5 flex items-center justify-between">
              <button
                type="button"
                className="flex items-center gap-3 text-left flex-1"
                onClick={() => setExpandedId(expandedId === swing.id ? null : swing.id)}
              >
                <div className={`text-2xl font-black ${getScoreColor(swing.overallScore)}`}>
                  {swing.overallScore.toFixed(1)}
                </div>
                <div>
                  <div className="font-black text-gray-900 capitalize">{swing.shotType}</div>
                  <div className="text-sm text-gray-500">{formatDate(swing.createdAt)}</div>
                </div>
              </button>
              <div className="flex items-center gap-3">
                <span className={`text-xs font-bold px-2 py-1 rounded-full border-2 ${getScoreBadgeColor(swing.overallScore)}`}>
                  {swing.overallScore >= 8 ? "GREAT" : swing.overallScore >= 6 ? "GOOD" : "NEEDS WORK"}
                </span>
                {canEdit && (
                  <button
                    type="button"
                    onClick={() => onDelete(swing.id)}
                    className="text-sm text-red-500 hover:text-red-700 font-semibold"
                  >
                    Delete
                  </button>
                )}
              </div>
            </div>

            {expandedId === swing.id && (
              <div className="px-5 pb-5 border-t-2 border-green-100 pt-4 space-y-4">
                <div className="grid grid-cols-1 gap-2">
                  {swing.categories.map((cat, i) => (
                    <div key={i} className="flex items-center justify-between">
                      <span className="text-sm font-semibold text-gray-700">{cat.name}</span>
                      <div className="flex items-center gap-2">
                        <div className="w-24 h-2 bg-gray-100 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full ${cat.score >= 8 ? "bg-green-500" : cat.score >= 6 ? "bg-yellow-400" : "bg-red-400"}`}
                            style={{ width: `${cat.score * 10}%` }}
                          />
                        </div>
                        <span className={`text-sm font-black w-8 text-right ${getSeverityColor(cat.severity)}`}>
                          {cat.score}/10
                        </span>
                      </div>
                    </div>
                  ))}
                </div>

                <div className="bg-green-50 border-2 border-green-300 rounded-lg p-3">
                  <div className="text-xs font-bold text-green-700 uppercase tracking-wide mb-1">Top Priority</div>
                  <p className="text-sm text-green-900">{swing.topPriority}</p>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
