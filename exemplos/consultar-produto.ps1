param(
    [int]$Codigo = 1,
    [string]$Url = 'http://localhost:8080/ws',
    [switch]$GerarXml
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($env:SOAP_SECURITY_USERNAME) -or
    [string]::IsNullOrWhiteSpace($env:SOAP_SECURITY_PASSWORD)) {
    throw 'Defina SOAP_SECURITY_USERNAME e SOAP_SECURITY_PASSWORD com as credenciais configuradas no servidor.'
}

# PasswordDigest do perfil UsernameToken: Base64(SHA1(nonce + created + senha)).
$nonce = New-Object byte[] 16
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $random.GetBytes($nonce) } finally { $random.Dispose() }
$now = [DateTime]::UtcNow
$created = $now.ToString("yyyy-MM-ddTHH:mm:ss.fffZ", [Globalization.CultureInfo]::InvariantCulture)
$expires = $now.AddMinutes(5).ToString("yyyy-MM-ddTHH:mm:ss.fffZ", [Globalization.CultureInfo]::InvariantCulture)
$bytes = [byte[]]($nonce + [Text.Encoding]::UTF8.GetBytes($created + $env:SOAP_SECURITY_PASSWORD))
$sha1 = [System.Security.Cryptography.SHA1]::Create()
try { $digest = [Convert]::ToBase64String($sha1.ComputeHash($bytes)) } finally { $sha1.Dispose() }
$nonceBase64 = [Convert]::ToBase64String($nonce)
$username = [System.Security.SecurityElement]::Escape($env:SOAP_SECURITY_USERNAME)

$template = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'consultar-produto.xml') -Raw -Encoding UTF8
$xml = $template.Replace('{{USERNAME}}', $username).Replace('{{PASSWORD_DIGEST}}', $digest).
    Replace('{{NONCE_BASE64}}', $nonceBase64).Replace('{{CREATED}}', $created).
    Replace('{{EXPIRES}}', $expires).Replace('<prod:codigo>1</prod:codigo>', "<prod:codigo>$Codigo</prod:codigo>")

if ($GerarXml) {
    $xml
}
else {
    try {
        $response = Invoke-WebRequest -Uri $Url -Method Post -ContentType 'text/xml; charset=UTF-8' `
            -Body ([Text.Encoding]::UTF8.GetBytes($xml)) -UseBasicParsing
        $response.Content
    }
    catch {
        if ($_.ErrorDetails.Message) { Write-Output $_.ErrorDetails.Message }
        throw
    }
}
