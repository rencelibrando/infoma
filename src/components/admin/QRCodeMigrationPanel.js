import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { 
  migrateBikesToQRCodeField, 
  validateBikeQRCodes, 
  generateQRCodeReport,
  fixIncorrectMaintenanceStatus
} from '../../services/migrationService';

const colors = {
  primary: '#2196F3',
  success: '#4CAF50',
  warning: '#FF9800',
  error: '#F44336',
  background: '#f5f5f5',
  white: '#ffffff',
  text: '#333333',
  border: '#ddd'
};

const Panel = styled.div`
  background: ${colors.white};
  border-radius: 8px;
  padding: 24px;
  margin: 16px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
`;

const PanelTitle = styled.h2`
  color: ${colors.text};
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: 600;
`;

const SectionTitle = styled.h3`
  color: ${colors.text};
  margin: 24px 0 12px 0;
  font-size: 18px;
  font-weight: 500;
`;

const ButtonGroup = styled.div`
  display: flex;
  gap: 12px;
  margin: 16px 0;
  flex-wrap: wrap;
`;

const Button = styled.button`
  padding: 12px 24px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 140px;
  
  ${props => {
    switch (props.variant) {
      case 'primary':
        return `
          background: ${colors.primary};
          color: white;
          &:hover { background: #1976D2; }
        `;
      case 'success':
        return `
          background: ${colors.success};
          color: white;
          &:hover { background: #45a049; }
        `;
      case 'warning':
        return `
          background: ${colors.warning};
          color: white;
          &:hover { background: #f57c00; }
        `;
      case 'danger':
        return `
          background: ${colors.error};
          color: white;
          &:hover { background: #d32f2f; }
        `;
      default:
        return `
          background: ${colors.background};
          color: ${colors.text};
          border: 1px solid ${colors.border};
          &:hover { background: #e0e0e0; }
        `;
    }
  }}
  
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

const StatusBox = styled.div`
  padding: 16px;
  border-radius: 6px;
  margin: 16px 0;
  border-left: 4px solid;
  
  ${props => {
    switch (props.type) {
      case 'success':
        return `
          background: #e8f5e8;
          border-color: ${colors.success};
          color: #2e7d32;
        `;
      case 'warning':
        return `
          background: #fff3e0;
          border-color: ${colors.warning};
          color: #ef6c00;
        `;
      case 'error':
        return `
          background: #ffebee;
          border-color: ${colors.error};
          color: #c62828;
        `;
      default:
        return `
          background: #e3f2fd;
          border-color: ${colors.primary};
          color: #1565c0;
        `;
    }
  }}
`;

const ReportContainer = styled.div`
  background: ${colors.background};
  border-radius: 6px;
  padding: 16px;
  margin: 16px 0;
`;

const ReportGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin: 16px 0;
`;

const StatCard = styled.div`
  background: ${colors.white};
  padding: 16px;
  border-radius: 6px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
`;

const StatNumber = styled.div`
  font-size: 24px;
  font-weight: bold;
  color: ${colors.primary};
  margin-bottom: 4px;
`;

const StatLabel = styled.div`
  font-size: 14px;
  color: ${colors.text};
`;

const IssuesList = styled.div`
  background: ${colors.white};
  border-radius: 6px;
  margin: 16px 0;
`;

const IssueItem = styled.div`
  padding: 12px 16px;
  border-bottom: 1px solid ${colors.border};
  
  &:last-child {
    border-bottom: none;
  }
`;

const IssueType = styled.span`
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  margin-right: 8px;
  
  ${props => {
    switch (props.type) {
      case 'duplicate_qrCode':
        return `background: #ffebee; color: #c62828;`;
      case 'duplicate_hardwareId':
        return `background: #fff3e0; color: #ef6c00;`;
      case 'missing_identifiers':
        return `background: #fce4ec; color: #ad1457;`;
      case 'identifier_mismatch':
        return `background: #e8eaf6; color: #3f51b5;`;
      default:
        return `background: ${colors.background}; color: ${colors.text};`;
    }
  }}
`;

const LoadingSpinner = styled.div`
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid ${colors.border};
  border-radius: 50%;
  border-top-color: ${colors.primary};
  animation: spin 1s ease-in-out infinite;
  margin-right: 8px;
  
  @keyframes spin {
    to { transform: rotate(360deg); }
  }
`;

const QRCodeMigrationPanel = () => {
  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(false);
  const [migrating, setMigrating] = useState(false);
  const [report, setReport] = useState(null);
  const [migrationResult, setMigrationResult] = useState(null);
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('info');
  const [validationReport, setValidationReport] = useState(null);
  const [maintenanceFixStatus, setMaintenanceFixStatus] = useState('');

  // Generate initial report on component mount
  useEffect(() => {
    handleGenerateReport();
  }, []);

  const handleMigration = async () => {
    try {
      setMigrating(true);
      setError(null);
      setMigrationResult(null);
      
      const result = await migrateBikesToQRCodeField();
      setMigrationResult(result);
      
      // Refresh report after migration
      await handleGenerateReport();
      
    } catch (err) {
      setError(`Migration failed: ${err.message}`);
    } finally {
      setMigrating(false);
    }
  };

  const handleValidation = async () => {
    try {
      setValidating(true);
      setError(null);
      
      const validation = await validateBikeQRCodes();
      
      // Update report with validation results
      setReport(prev => ({
        ...prev,
        details: validation,
        timestamp: new Date().toISOString()
      }));
      
    } catch (err) {
      setError(`Validation failed: ${err.message}`);
    } finally {
      setValidating(false);
    }
  };

  const handleGenerateReport = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const report = await generateQRCodeReport();
      setValidationReport(report);
      setMessage('QR Code report generated successfully');
      setMessageType('success');
    } catch (error) {
      console.error('Report generation failed:', error);
      setError(`Report generation failed: ${error.message}`);
      setMessageType('error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleFixMaintenanceStatus = async () => {
    setIsLoading(true);
    setMaintenanceFixStatus('Fixing incorrect maintenance statuses...');
    try {
      const result = await fixIncorrectMaintenanceStatus();
      if (result.success) {
        setMaintenanceFixStatus(`✅ Fixed ${result.fixed} bikes with incorrect maintenance status`);
        setMessageType('success');
        
        // Refresh validation report if it exists
        if (validationReport) {
          const newReport = await generateQRCodeReport();
          setValidationReport(newReport);
        }
      } else {
        setMaintenanceFixStatus(`❌ Fix failed: ${result.error}`);
        setMessageType('error');
      }
    } catch (error) {
      console.error('Maintenance status fix failed:', error);
      setMaintenanceFixStatus(`❌ Fix failed: ${error.message}`);
      setMessageType('error');
    } finally {
      setIsLoading(false);
    }
  };

  const formatIssueMessage = (issue) => {
    switch (issue.type) {
      case 'duplicate_qrCode':
        return `Duplicate QR code "${issue.qrCode}" found in bikes: ${issue.bikes.join(', ')}`;
      case 'duplicate_hardwareId':
        return `Duplicate hardware ID "${issue.hardwareId}" found in bikes: ${issue.bikes.join(', ')}`;
      case 'missing_identifiers':
        return `Bike "${issue.bikeName || issue.bikeId}" has no QR code or hardware ID`;
      case 'identifier_mismatch':
        return `Bike ${issue.bikeId}: QR code (${issue.qrCode}) differs from hardware ID (${issue.hardwareId})`;
      default:
        return JSON.stringify(issue);
    }
  };

  return (
    <Panel>
      <PanelTitle>QR Code Migration & Management</PanelTitle>
      
      {error && (
        <StatusBox type="error">
          <strong>Error:</strong> {error}
        </StatusBox>
      )}

      {/* Action Buttons */}
      <ButtonGroup>
        <Button 
          variant="primary" 
          onClick={handleGenerateReport}
          disabled={loading}
        >
          {loading && <LoadingSpinner />}
          Generate Report
        </Button>
        
        <Button 
          variant="warning" 
          onClick={handleValidation}
          disabled={validating}
        >
          {validating && <LoadingSpinner />}
          Validate QR Codes
        </Button>
        
        <Button 
          variant="success" 
          onClick={handleMigration}
          disabled={migrating || (report && report.summary && report.summary.totalBikes === 0)}
        >
          {migrating && <LoadingSpinner />}
          Migrate Bikes
        </Button>
        
        <Button 
          variant="danger" 
          onClick={handleFixMaintenanceStatus}
          disabled={isLoading}
        >
          {isLoading && <LoadingSpinner />}
          Fix Maintenance Status
        </Button>
      </ButtonGroup>

      {/* Migration Result */}
      {migrationResult && (
        <StatusBox type={migrationResult.success ? "success" : "error"}>
          <strong>Migration Result:</strong><br />
          {migrationResult.success ? (
            <>
              Successfully processed {migrationResult.totalBikes} bikes:<br />
              • Migrated: {migrationResult.migratedCount}<br />
              • Skipped: {migrationResult.skippedCount}
            </>
          ) : (
            `Migration failed: ${migrationResult.error}`
          )}
        </StatusBox>
      )}

      {/* Report Summary */}
      {report && report.summary && (
        <>
          <SectionTitle>Current Status</SectionTitle>
          <ReportGrid>
            <StatCard>
              <StatNumber>{report.summary.totalBikes}</StatNumber>
              <StatLabel>Total Bikes</StatLabel>
            </StatCard>
            <StatCard>
              <StatNumber>{report.summary.bikesWithQRCode}</StatNumber>
              <StatLabel>With QR Code</StatLabel>
            </StatCard>
            <StatCard>
              <StatNumber>{report.summary.bikesWithHardwareId}</StatNumber>
              <StatLabel>With Hardware ID</StatLabel>
            </StatCard>
            <StatCard>
              <StatNumber>{report.summary.bikesWithoutIdentifiers}</StatNumber>
              <StatLabel>Missing Both</StatLabel>
            </StatCard>
            <StatCard>
              <StatNumber>{report.summary.duplicateQRCodes}</StatNumber>
              <StatLabel>Duplicate QR Codes</StatLabel>
            </StatCard>
            <StatCard>
              <StatNumber>{report.summary.totalIssues}</StatNumber>
              <StatLabel>Total Issues</StatLabel>
            </StatCard>
          </ReportGrid>

          {/* Recommendations */}
          {report.recommendations && report.recommendations.length > 0 && (
            <>
              <SectionTitle>Recommendations</SectionTitle>
              {report.recommendations.map((rec, index) => (
                <StatusBox key={index} type="info">
                  {rec}
                </StatusBox>
              ))}
            </>
          )}

          {/* Issues Detail */}
          {report.details && report.details.issues && report.details.issues.length > 0 && (
            <>
              <SectionTitle>Issues Found ({report.details.issues.length})</SectionTitle>
              <IssuesList>
                {report.details.issues.map((issue, index) => (
                  <IssueItem key={index}>
                    <IssueType type={issue.type}>
                      {issue.type.replace('_', ' ').toUpperCase()}
                    </IssueType>
                    {formatIssueMessage(issue)}
                  </IssueItem>
                ))}
              </IssuesList>
            </>
          )}

          {/* Report Timestamp */}
          <StatusBox type="info">
            <small>Report generated: {new Date(report.timestamp).toLocaleString()}</small>
          </StatusBox>
        </>
      )}

      {/* Maintenance Fix Status */}
      {maintenanceFixStatus && (
        <StatusBox type={maintenanceFixStatus.includes('✅') ? 'success' : maintenanceFixStatus.includes('❌') ? 'error' : 'info'}>
          {maintenanceFixStatus}
        </StatusBox>
      )}
    </Panel>
  );
};

export default QRCodeMigrationPanel; 