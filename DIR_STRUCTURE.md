# Directory Structure

```
/home/sive/problems/withdrawal_system/
├───.gitignore
├───pom.xml
├───README.md
├───README.txt
├───.git/...
├───.idea/...
├───src/
│   ├───main/
│   │   ├───java/
│   │   │   └───com/
│   │   │       └───enviro/
│   │   │           └───assessment/
│   │   │               └───junior/
│   │   │                   └───sive/
│   │   │                       ├───Enviro365Application.java
│   │   │                       ├───bootstrap/
│   │   │                       │   └───DataLoader.java
│   │   │                       ├───config/
│   │   │                       │   └───WebConfig.java
│   │   │                       ├───controller/
│   │   │                       │   ├───ExportController.java
│   │   │                       │   ├───InvestorController.java
│   │   │                       │   ├───InvestorViewController.java
│   │   │                       │   ├───ProductController.java
│   │   │                       │   └───WithdrawalController.java
│   │   │                       ├───dto/
│   │   │                       │   ├───CreateHoldingRequestDto.java
│   │   │                       │   ├───CreateInvestorRequestDto.java
│   │   │                       │   ├───ErrorResponseDto.java
│   │   │                       │   ├───HoldingDto.java
│   │   │                       │   ├───InvestorDto.java
│   │   │                       │   ├───PortfolioResponseDto.java
│   │   │                       │   ├───ProductDto.java
│   │   │                       │   ├───WithdrawalRequestDto.java
│   │   │                       │   └───WithdrawalResponseDto.java
│   │   │                       ├───entity/
│   │   │                       │   ├───Holding.java
│   │   │                       │   ├───Investor.java
│   │   │                       │   ├───Product.java
│   │   │                       │   ├───ProductType.java
│   │   │                       │   ├───WithdrawalNotice.java
│   │   │                       │   └───WithdrawalStatus.java
│   │   │                       ├───exception/
│   │   │                       │   ├───BusinessRuleException.java
│   │   │                       │   ├───DuplicateResourceException.java
│   │   │                       │   ├───GlobalExceptionHandler.java
│   │   │                       │   └───ResourceNotFoundException.java
│   │   │                       ├───repository/
│   │   │                       │   ├───HoldingRepository.java
│   │   │                       │   ├───InvestorRepository.java
│   │   │                       │   ├───ProductRepository.java
│   │   │                       │   └───WithdrawalNoticeRepository.java
│   │   │                       └───service/
│   │   │                           ├───CsvExportService.java
│   │   │                           ├───PortfolioService.java
│   │   │                           └───WithdrawalService.java
│   │   └───resources/
│   │       ├───application.properties
│   │       ├───static/
│   │       │   ├───css/
│   │       │   │   └───style.css
│   │       │   └───js/
│   │       │       ├───admin.js
│   │       │       ├───app.js
│   │       │       ├───auth.js
│   │       │       ├───login.js
│   │       │       └───settings.js
│   │       └───templates/
│   │           ├───admin.html
│   │           ├───index.html
│   │           ├───login.html
│   │           └───settings.html
│   └───test/
│       └───java/
│           └───com/
│               └───enviro/
│                   └───assessment/
│                       └───junior/
│                           └───sive/
│                               └───service/
│                                   └───WithdrawalServiceTest.java
|                                   └───PortfolioServiceTest.java
|                                   └───CsvExportServiceTest.java
└───target/...
```
