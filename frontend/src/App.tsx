import { QRCodeSVG } from "qrcode.react";

function App() {
  const otpauthUri = "otpauth://totp/Krob%20Krong%3Aadmin%40krobkrong.com?secret=SCORYH7OJXZSO4U72WATW63PVYNV2H55&issuer=Krob%20Krong&algorithm=SHA1&digits=6&period=30";

  return (
    <main>
      <h1>Krob Krong MFA Setup</h1>
      <p>Scan this QR code with Google Authenticator.</p>

      <QRCodeSVG
        value={otpauthUri}
        size={240}
      />
    </main>
  );
}

export default App;