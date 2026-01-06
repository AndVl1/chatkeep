import React from 'react';
import ReactDOM from 'react-dom/client';
// Mock environment MUST be imported before SDK init
import './mockEnv';
import { App } from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
