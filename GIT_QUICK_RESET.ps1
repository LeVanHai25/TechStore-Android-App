# Script PowerShell để gỡ Git và tạo lại từ đầu
# Chạy script này trong PowerShell: .\GIT_QUICK_RESET.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Gỡ Git và tạo lại repository mới" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Kiểm tra xem có Git không
if (Test-Path .git) {
    Write-Host "[1/5] Đang xóa Git repository cũ..." -ForegroundColor Yellow
    Remove-Item -Recurse -Force .git
    Write-Host "      ✓ Đã xóa Git cũ" -ForegroundColor Green
} else {
    Write-Host "[1/5] Không tìm thấy Git repository" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[2/5] Đang khởi tạo Git repository mới..." -ForegroundColor Yellow
git init
Write-Host "      ✓ Đã khởi tạo Git mới" -ForegroundColor Green

Write-Host ""
Write-Host "[3/5] Đang thêm tất cả files..." -ForegroundColor Yellow
git add .
Write-Host "      ✓ Đã thêm files" -ForegroundColor Green

Write-Host ""
Write-Host "[4/5] Đang commit lần đầu..." -ForegroundColor Yellow
git commit -m "Initial commit - TechStore Android App"
Write-Host "      ✓ Đã commit" -ForegroundColor Green

Write-Host ""
Write-Host "[5/5] Hoàn thành!" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  BƯỚC TIẾP THEO:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Tạo repository mới trên GitHub/GitLab" -ForegroundColor White
Write-Host "2. Chạy các lệnh sau:" -ForegroundColor White
Write-Host ""
Write-Host "   git remote add origin <URL_REPOSITORY>" -ForegroundColor Yellow
Write-Host "   git branch -M main" -ForegroundColor Yellow
Write-Host "   git push -u origin main" -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan


