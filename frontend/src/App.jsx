import React, { useCallback, useEffect, useState } from "react";
import { api } from "./api/client.js";
import { AuthProvider, useAuth } from "./context/AuthContext.jsx";
import LoginBar from "./components/LoginBar.jsx";
import UploadPanel from "./components/UploadPanel.jsx";
import CoachingReport from "./components/CoachingReport.jsx";
import SwingHistory from "./components/SwingHistory.jsx";

function BedererApp() {
  const { credentials, isAuthenticated } = useAuth();
  const [activeTab, setActiveTab] = useState("analyze");
  const [history, setHistory] = useState([]);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysis, setAnalysis] = useState(null);
  const [error, setError] = useState(null);

  const loadHistory = useCallback(async () => {
    try {
      const data = await api.getHistory();
      setHistory(data);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    loadHistory();
  }, [loadHistory]);

  async function handleAnalyze(file, shotType) {
    if (!file) return;
    setIsAnalyzing(true);
    setError(null);
    try {
      const result = await api.analyzeSwing(file, shotType, credentials);
      setAnalysis(result);
      await loadHistory();
    } catch (err) {
      setError(err.message);
    } finally {
      setIsAnalyzing(false);
    }
  }

  async function handleDelete(id) {
    try {
      await api.deleteSwing(id, credentials);
      await loadHistory();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 via-gray-50 to-green-100">
      <div className="bg-gradient-to-r from-green-800 to-green-700 text-white py-8 mb-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h1 className="text-4xl font-black mb-2">🎾 Bederer AI</h1>
          <p className="text-green-100">Upload your swing and get instant AI coaching feedback</p>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-6">
        <LoginBar />

        <div className="flex gap-2 bg-white rounded-xl shadow p-1.5 border-2 border-green-200 w-fit">
          <button
            type="button"
            onClick={() => setActiveTab("analyze")}
            className={`px-6 py-2.5 rounded-lg font-bold text-sm transition-all ${activeTab === "analyze" ? "bg-green-600 text-white shadow" : "text-gray-600 hover:bg-green-50"}`}
          >
            Analyze Swing
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("history")}
            className={`px-6 py-2.5 rounded-lg font-bold text-sm transition-all flex items-center gap-2 ${activeTab === "history" ? "bg-green-600 text-white shadow" : "text-gray-600 hover:bg-green-50"}`}
          >
            My Swings
            {history.length > 0 && (
              <span className="text-xs px-2 py-0.5 rounded-full font-black bg-green-100 text-green-700">
                {history.length}
              </span>
            )}
          </button>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pb-12">
        {activeTab === "history" ? (
          <SwingHistory history={history} onDelete={handleDelete} canEdit={isAuthenticated} />
        ) : (
          <div className="grid lg:grid-cols-2 gap-8">
            <div className="lg:sticky lg:top-8 lg:self-start">
              <UploadPanel
                onAnalyze={handleAnalyze}
                isAnalyzing={isAnalyzing}
                disabled={!isAuthenticated}
                error={error}
              />
            </div>
            <div>
              {isAnalyzing ? (
                <div className="bg-white rounded-xl shadow-2xl p-12 border-4 border-green-600 text-center">
                  <div className="text-5xl mb-3 animate-bounce">🎾</div>
                  <h3 className="text-2xl font-black text-gray-900">Coaching in Progress</h3>
                  <p className="text-gray-500 mt-1 text-sm">This usually takes 20-30 seconds</p>
                </div>
              ) : analysis ? (
                <CoachingReport analysis={analysis} />
              ) : (
                <div className="bg-white rounded-xl shadow-2xl p-12 border-4 border-dashed border-green-300 text-center">
                  <div className="text-6xl mb-4">🎾</div>
                  <h3 className="text-2xl font-bold text-gray-900 mb-2">Your Coaching Report Will Appear Here</h3>
                  <p className="text-gray-600">Upload a video and click "Analyze My Swing" to get started</p>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BedererApp />
    </AuthProvider>
  );
}
