import React, { useRef, useState } from "react";

const MAX_FILE_SIZE_MB = 500;
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

const SHOTS = [
  { value: "forehand", label: "Forehand" },
  { value: "backhand", label: "Backhand" },
  { value: "serve", label: "Serve" },
  { value: "volley", label: "Volley" },
];

function formatFileSize(bytes) {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function UploadPanel({ onAnalyze, isAnalyzing, disabled, error }) {
  const [selectedFile, setSelectedFile] = useState(null);
  const [videoPreview, setVideoPreview] = useState(null);
  const [shotType, setShotType] = useState("forehand");
  const [fileError, setFileError] = useState(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef(null);

  function validateAndSetFile(file) {
    setFileError(null);

    if (!file.type.startsWith("video/")) {
      setFileError("That doesn't look like a video file. Please upload an MP4, MOV, or similar video format.");
      return;
    }
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setFileError(`File is too large (${formatFileSize(file.size)}). Please upload a video under ${MAX_FILE_SIZE_MB}MB.`);
      return;
    }

    setSelectedFile(file);
    setVideoPreview(URL.createObjectURL(file));
  }

  function handleDrop(e) {
    e.preventDefault();
    setIsDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) validateAndSetFile(file);
  }

  return (
    <div className="bg-white rounded-xl shadow-2xl p-8 border-4 border-green-600 space-y-6">
      <div>
        <label className="block text-lg font-bold text-gray-900 mb-3">Upload Your Swing Video</label>
        <div
          onDrop={handleDrop}
          onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
          onDragLeave={() => setIsDragOver(false)}
          onClick={() => fileInputRef.current?.click()}
          className={`relative cursor-pointer rounded-xl border-4 border-dashed p-8 text-center transition-all
            ${isDragOver ? "border-green-500 bg-green-50" : selectedFile && !fileError ? "border-green-400 bg-green-50" : "border-gray-300 bg-gray-50 hover:border-green-400 hover:bg-green-50"}`}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept="video/*"
            onChange={(e) => e.target.files?.[0] && validateAndSetFile(e.target.files[0])}
            className="hidden"
          />
          {selectedFile && !fileError ? (
            <>
              <p className="font-bold text-green-800 text-sm truncate px-4">{selectedFile.name}</p>
              <p className="text-green-600 text-xs mt-1">{formatFileSize(selectedFile.size)} · Click to change</p>
            </>
          ) : (
            <>
              <p className="font-black text-gray-700 text-base">Drag & drop your video here</p>
              <p className="text-gray-400 text-sm mt-1">or click to browse files</p>
              <p className="text-gray-400 text-xs mt-3">MP4, MOV, AVI · Max {MAX_FILE_SIZE_MB}MB</p>
            </>
          )}
        </div>
        {fileError && (
          <div className="mt-3 bg-red-50 border-2 border-red-300 text-red-800 px-4 py-3 rounded-lg text-sm font-medium">
            {fileError}
          </div>
        )}
      </div>

      <div>
        <label className="block text-lg font-bold text-gray-900 mb-3">What Shot Are You Hitting?</label>
        <div className="grid grid-cols-2 gap-3">
          {SHOTS.map((shot) => (
            <button
              key={shot.value}
              type="button"
              onClick={() => setShotType(shot.value)}
              className={`p-4 rounded-lg border-2 font-bold transition-all
                ${shotType === shot.value ? "bg-green-600 text-white border-green-700 shadow-lg" : "bg-white text-gray-700 border-gray-300 hover:border-green-400 hover:bg-green-50"}`}
            >
              {shot.label}
            </button>
          ))}
        </div>
      </div>

      {videoPreview && (
        <div>
          <label className="block text-lg font-bold text-gray-900 mb-3">Preview</label>
          <video src={videoPreview} controls className="w-full rounded-lg shadow-lg border-4 border-green-200 max-h-96" />
        </div>
      )}

      <button
        type="button"
        onClick={() => onAnalyze(selectedFile, shotType)}
        disabled={!selectedFile || isAnalyzing || !!fileError || disabled}
        className="w-full bg-gradient-to-r from-green-600 to-green-700 text-white py-4 px-6 rounded-xl font-black text-lg
          hover:from-green-700 hover:to-green-800 disabled:from-gray-300 disabled:to-gray-400 disabled:cursor-not-allowed
          transition-all shadow-xl border-4 border-green-800 disabled:border-gray-500"
      >
        {isAnalyzing ? "Analyzing Your Swing..." : disabled ? "Sign in to analyze" : "Analyze My Swing"}
      </button>

      {error && (
        <div className="bg-red-50 border-4 border-red-300 text-red-800 px-6 py-4 rounded-lg font-medium">
          {error}
        </div>
      )}
    </div>
  );
}
