import React, { useState, useEffect } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import QRCode from 'qrcode';
import styled from 'styled-components';
import { FiPrinter } from 'react-icons/fi';

// Pine green and gray theme colors
const colors = {
  pineGreen: '#1D3C34',
  lightPineGreen: '#2D5A4C',
  darkGray: '#333333',
  mediumGray: '#666666',
  lightGray: '#f2f2f2',
  white: '#ffffff',
  warning: '#FFC107',
  success: '#4CAF50',
};

const QRContainer = styled.div`
  background-color: ${colors.white};
  border-radius: 8px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  min-width: 300px;
`;

const BikeName = styled.h3`
  margin: 15px 0 5px;
  color: ${colors.darkGray};
  text-align: center;
  font-size: 18px;
  font-weight: 600;
`;

const BikeId = styled.p`
  margin: 5px 0 5px;
  color: ${colors.mediumGray};
  font-size: 14px;
  text-align: center;
  font-family: 'Courier New', monospace;
`;

const QRCodeValue = styled.p`
  margin: 5px 0 15px;
  color: ${colors.pineGreen};
  font-size: 16px;
  text-align: center;
  font-family: 'Courier New', monospace;
  font-weight: bold;
  word-break: break-all;
  max-width: 200px;
`;

const QRLabel = styled.div`
  margin-top: 15px;
  font-size: 12px;
  color: ${colors.mediumGray};
  text-align: center;
`;

const StatusContainer = styled.div`
  display: flex;
  gap: 10px;
  margin: 10px 0;
  flex-wrap: wrap;
  justify-content: center;
`;

const StatusLabel = styled.div`
  background-color: ${props => {
    if (props.type === 'locked') return props.value ? colors.warning : colors.success;
    if (props.type === 'available') return props.value ? colors.success : colors.mediumGray;
    if (props.type === 'inUse') return props.value ? '#ff5722' : colors.success;
    return colors.lightGray;
  }};
  color: ${colors.white};
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
`;

const StatusIcon = styled.span`
  margin-right: 5px;
  font-size: 12px;
`;

const WarningText = styled.p`
  margin: 10px 0;
  color: ${colors.warning};
  font-size: 12px;
  text-align: center;
  font-style: italic;
`;

const PrintButton = styled.button`
  background-color: ${colors.pineGreen};
  color: ${colors.white};
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 15px;
  transition: all 0.3s ease;
  
  &:hover {
    background-color: ${colors.lightPineGreen};
    transform: translateY(-1px);
  }
  
  &:active {
    transform: translateY(0);
  }
`;

const LoadingContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  width: 200px;
`;

const LoadingSpinner = styled.div`
  width: 40px;
  height: 40px;
  border: 4px solid ${colors.lightGray};
  border-top: 4px solid ${colors.pineGreen};
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
`;

const LoadingText = styled.div`
  color: ${colors.mediumGray};
  font-size: 14px;
  text-align: center;
  font-style: italic;
`;

const QRCodeContainer = styled.div`
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  transition: opacity 0.3s ease;
`;

const BikeQRCode = ({ bike }) => {
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  useEffect(() => {
    // Simulate QR code generation time
    const timer = setTimeout(() => {
      setIsLoading(false);
    }, 500); // 0.5 second loading time

    return () => clearTimeout(timer);
  }, []);

  if (!bike) {
    return null;
  }

  // Get the effective QR code (prefer qrCode over hardwareId)
  const effectiveQRCode = bike.qrCode || bike.hardwareId;
  
  if (!effectiveQRCode) {
    return (
      <QRContainer>
        <BikeName>{bike.name}</BikeName>
        <WarningText>No QR code assigned to this bike</WarningText>
      </QRContainer>
    );
  }

  // Generate QR code value - use simple string format for mobile app compatibility
  const qrValue = effectiveQRCode;

  // Handle print QR code
  const handlePrint = async () => {
    const qrCodeData = effectiveQRCode;
    const bikeName = bike.name || 'Unnamed Bike';
    
    try {
      // Generate QR code as high-resolution data URL using the npm package
      const url = await QRCode.toDataURL(qrCodeData, {
        width: 400, // Higher resolution for better print quality
        height: 400,
        color: {
          dark: '#000000', // Pure black for best print contrast
          light: '#FFFFFF'
        },
        margin: 1, // Minimal margin
        errorCorrectionLevel: 'H' // High error correction for print reliability
      });

      // Create minimal print window with only the QR code
      const printWindow = window.open('', '_blank', 'width=600,height=600');
      const printContent = `
        <!DOCTYPE html>
        <html>
        <head>
          <title>QR Code - ${bikeName}</title>
          <style>
            * {
              margin: 0;
              padding: 0;
              box-sizing: border-box;
            }
            html, body {
              width: 100%;
              height: 100%;
              background: white;
            }
            body {
              display: flex;
              align-items: center;
              justify-content: center;
              font-family: Arial, sans-serif;
            }
            .qr-print-container {
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              min-height: 100vh;
              padding: 20px;
            }
            .qr-image {
              max-width: 100%;
              max-height: 100%;
              width: auto;
              height: auto;
              display: block;
            }
            .bike-label {
              font-size: 12px;
              color: #333;
              margin-top: 10px;
              text-align: center;
              font-weight: normal;
            }
            @media print {
              html, body {
                width: 100%;
                height: 100%;
                margin: 0;
                padding: 0;
                background: white !important;
                -webkit-print-color-adjust: exact;
                color-adjust: exact;
              }
              .qr-print-container {
                min-height: 100vh;
                padding: 0;
                display: flex;
                align-items: center;
                justify-content: center;
              }
              .qr-image {
                max-width: 90vmin;
                max-height: 90vmin;
                width: auto;
                height: auto;
              }
              .bike-label {
                font-size: 10px;
                margin-top: 8px;
              }
              @page {
                margin: 0.5in;
                size: auto;
              }
            }
          </style>
        </head>
        <body>
          <div class="qr-print-container">
            <img src="${url}" alt="QR Code for ${bikeName}" class="qr-image" />
            <div class="bike-label">${bikeName}</div>
          </div>
          <script>
            window.onload = function() {
              // Small delay to ensure image is fully loaded
              setTimeout(() => {
                window.print();
                // Close window after printing (optional)
                setTimeout(() => {
                  window.close();
                }, 1000);
              }, 300);
            };
          </script>
        </body>
        </html>
      `;
      
      printWindow.document.write(printContent);
      printWindow.document.close();
    } catch (error) {
      console.error('QR Code generation error:', error);
      alert('Failed to generate QR code for printing. Please try again.');
    }
  };

  return (
    <QRContainer>
      <BikeName>{bike.name}</BikeName>
      <BikeId>Bike ID: {bike.id}</BikeId>
      <QRCodeValue>{effectiveQRCode}</QRCodeValue>
      
      <StatusContainer>
        <StatusLabel type="locked" value={bike.isLocked}>
          <StatusIcon>{bike.isLocked ? '🔒' : '🔓'}</StatusIcon>
          {bike.isLocked ? 'Locked' : 'Unlocked'}
        </StatusLabel>
        
        <StatusLabel type="available" value={bike.isAvailable}>
          <StatusIcon>{bike.isAvailable ? '✅' : '❌'}</StatusIcon>
          {bike.isAvailable ? 'Available' : 'Unavailable'}
        </StatusLabel>
        
        <StatusLabel type="inUse" value={bike.isInUse}>
          <StatusIcon>{bike.isInUse ? '🚴' : '🅿️'}</StatusIcon>
          {bike.isInUse ? 'In Use' : 'Parked'}
        </StatusLabel>
      </StatusContainer>
      
      <QRCodeContainer>
        {isLoading ? (
          <LoadingContainer>
            <LoadingSpinner />
            <LoadingText>Generating QR code...</LoadingText>
          </LoadingContainer>
        ) : (
          <QRCodeSVG 
            value={qrValue}
            size={200}
            bgColor={colors.white}
            fgColor={colors.pineGreen}
            level="H"
            includeMargin={true}
          />
        )}
      </QRCodeContainer>
      
      {!isLoading && (
        <>
          <QRLabel>
            Scan this QR code with the mobile app to unlock
          </QRLabel>

          <PrintButton onClick={handlePrint} disabled={isLoading}>
            <FiPrinter size={16} />
            Print QR Code
          </PrintButton>
        </>
      )}
      
      {bike.qrCode && bike.hardwareId && bike.qrCode !== bike.hardwareId && (
        <WarningText>
          Note: This bike has both QR code and hardware ID. Using QR code.
        </WarningText>
      )}
      
      {!bike.qrCode && bike.hardwareId && (
        <WarningText>
          Note: Using legacy hardware ID. Consider updating to new QR code format.
        </WarningText>
      )}
    </QRContainer>
  );
};

export default BikeQRCode; 