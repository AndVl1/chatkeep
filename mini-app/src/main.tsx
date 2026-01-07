import React from 'react';
import ReactDOM from 'react-dom/client';
// Mock environment MUST be imported before SDK init
import './mockEnv';
// i18n MUST be imported before App
import './i18n';
import { App } from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
