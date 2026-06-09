$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$services = @(
    @{ Name = "users-service"; Jar = "users-service\target\users-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "products-service"; Jar = "products-service\target\products-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "orders-service"; Jar = "orders-service\target\orders-service-0.0.1-SNAPSHOT.jar" },
    @{ Name = "api-gateway"; Jar = "api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar" }
)

foreach ($service in $services) {
    $outLog = Join-Path $root "$($service.Name).out.log"
    $errLog = Join-Path $root "$($service.Name).err.log"
    $jarPath = Join-Path $root $service.Jar
    Start-Process -FilePath "java" `
        -ArgumentList "-jar `"$jarPath`"" `
        -WorkingDirectory $root `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -WindowStyle Hidden
}

Write-Host "Servicos iniciados. Gateway: http://localhost:5000"
Write-Host "Logs: *.out.log e *.err.log"
