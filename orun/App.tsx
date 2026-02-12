import React from 'react';
import { HashRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/Layout.tsx';
import { Dashboard } from './components/Dashboard.tsx';
import { InstanceList } from './components/InstanceList.tsx';
import { InstanceDetail } from './components/InstanceDetail.tsx';
import { JobList } from './components/JobList.tsx';
import { Metrics } from './components/Metrics.tsx';

function App() {
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/instances" element={<InstanceList />} />
          <Route path="/instances/:id" element={<InstanceDetail />} />
          <Route path="/jobs" element={<JobList />} />
          <Route path="/metrics" element={<Metrics />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Layout>
    </Router>
  );
}

export default App;
