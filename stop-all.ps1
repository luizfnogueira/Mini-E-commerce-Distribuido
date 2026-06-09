$ports = @(5000, 5001, 5002, 5003)
$connections = Get-NetTCPConnection -LocalPort $ports -ErrorAction SilentlyContinue
$processIds = $connections | Select-Object -ExpandProperty OwningProcess -Unique

foreach ($processId in $processIds) {
    Stop-Process -Id $processId -Force
    Write-Host "Processo encerrado: $processId"
}
