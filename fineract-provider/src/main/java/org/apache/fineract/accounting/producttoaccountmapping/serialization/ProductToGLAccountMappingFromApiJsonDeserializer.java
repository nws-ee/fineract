/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.accounting.producttoaccountmapping.serialization;

import static org.apache.fineract.portfolio.savings.SavingsApiConstants.SAVINGS_PRODUCT_RESOURCE_NAME;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.accountingRuleParamName;
import static org.apache.fineract.portfolio.savings.SavingsApiConstants.isDormancyTrackingActiveParamName;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.common.AccountingConstants.LoanProductAccountingParams;
import org.apache.fineract.accounting.common.AccountingConstants.SavingProductAccountingParams;
import org.apache.fineract.accounting.common.AccountingConstants.SharesProductAccountingParams;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.producttoaccountmapping.service.ProductToGLAccountMappingWritePlatformService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.savings.DepositAccountType;
import org.apache.fineract.portfolio.shareproducts.constants.ShareProductApiConstants;
import org.springframework.stereotype.Component;

/**
 * TODO Vishwas find a better approach for validation
 *
 * Currently, validation of the passed in JSON is done before calling save or update method on the target resource (in
 * our case the loan Product)
 *
 * However, in the case of a loan product it would be difficult to validate the passed in JSON for valid
 * {@link LoanProduct} to {@link GLAccount} mappings during update because of the following scenario
 *
 * The accounting rule type may be changed in the update command, so we would have to validate if all required account
 * heads for a particular account type have been passed in (would be different for CASH and Accrual based). However,
 * till we have access to the domain object it would not be possible to detect if an accounting rule has actually been
 * changed
 *
 * Hence, method {@link #validateForLoanProductCreate(String)} from this class is called separately for validation only
 * if an accounting rule change is detected by {@link ProductToGLAccountMappingWritePlatformService}
 *
 * Also, the class is probably named wrong (*FromApiJsonDeserializer) should probably be named as (*Validator) instead
 *
 */
@Component
@RequiredArgsConstructor
public final class ProductToGLAccountMappingFromApiJsonDeserializer {

    private final FromJsonHelper fromApiJsonHelper;

    public void validateForLoanProductCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loanproduct");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        // accounting related data validation
        final Integer accountingRuleType = this.fromApiJsonHelper.extractIntegerNamed("accountingRule", element, Locale.getDefault());
        baseDataValidator.reset().parameter("accountingRule").value(accountingRuleType).notNull().inMinMaxRange(1, 4);

        if (isCashBasedAccounting(accountingRuleType) || isAccrualBasedAccounting(accountingRuleType)) {

            final Long purshasedAssetId = this.fromApiJsonHelper.extractLongNamed(LoanProductAccountingParams.PURSHASED_ASSET.getValue(),
                    element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.PURSHASED_ASSET.getValue()).value(purshasedAssetId).notNull()
                    .integerGreaterThanZero();

            final Long fundAccountId = this.fromApiJsonHelper.extractLongNamed(LoanProductAccountingParams.FUND_SOURCE.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.FUND_SOURCE.getValue()).value(fundAccountId).notNull()
                    .integerGreaterThanZero();

            final Long loanPortfolioAccountId = this.fromApiJsonHelper
                    .extractLongNamed(LoanProductAccountingParams.LOAN_PORTFOLIO.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.LOAN_PORTFOLIO.getValue()).value(loanPortfolioAccountId)
                    .notNull().integerGreaterThanZero();

            final Long incomeFromInterestId = this.fromApiJsonHelper
                    .extractLongNamed(LoanProductAccountingParams.INTEREST_ON_LOANS.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.INTEREST_ON_LOANS.getValue()).value(incomeFromInterestId)
                    .notNull().integerGreaterThanZero();

            final Long incomeFromFeeId = this.fromApiJsonHelper.extractLongNamed(LoanProductAccountingParams.INCOME_FROM_FEES.getValue(),
                    element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.INCOME_FROM_FEES.getValue()).value(incomeFromFeeId).notNull()
                    .integerGreaterThanZero();

            final Long incomeFromPenaltyId = this.fromApiJsonHelper
                    .extractLongNamed(LoanProductAccountingParams.INCOME_FROM_PENALTIES.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.INCOME_FROM_PENALTIES.getValue()).value(incomeFromPenaltyId)
                    .notNull().integerGreaterThanZero();

            final Long incomeFromRecoveryAccountId = this.fromApiJsonHelper
                    .extractLongNamed(LoanProductAccountingParams.INCOME_FROM_RECOVERY.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.INCOME_FROM_RECOVERY.getValue())
                    .value(incomeFromRecoveryAccountId).notNull().integerGreaterThanZero();

            final Long writeOffAccountId = this.fromApiJsonHelper
                    .extractLongNamed(LoanProductAccountingParams.LOSSES_WRITTEN_OFF.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.LOSSES_WRITTEN_OFF.getValue()).value(writeOffAccountId)
                    .notNull().integerGreaterThanZero();

            final Long overpaymentAccountId = this.fromApiJsonHelper.extractLongNamed(LoanProductAccountingParams.OVERPAYMENT.getValue(),
                    element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.OVERPAYMENT.getValue()).value(overpaymentAccountId).notNull()
                    .integerGreaterThanZero();

            final Long transfersInSuspenseAccountId = this.fromApiJsonHelper
                    .extractLongNamed(LoanProductAccountingParams.TRANSFERS_SUSPENSE.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.TRANSFERS_SUSPENSE.getValue())
                    .value(transfersInSuspenseAccountId).notNull().integerGreaterThanZero();

        }

        if (isAccrualBasedAccounting(accountingRuleType)) {

            final Long receivableInterestAccountId = this.fromApiJsonHelper
                    .extractLongNamed(LoanProductAccountingParams.INTEREST_RECEIVABLE.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.INTEREST_RECEIVABLE.getValue())
                    .value(receivableInterestAccountId).notNull().integerGreaterThanZero();

            final Long receivableFeeAccountId = this.fromApiJsonHelper
                    .extractLongNamed(LoanProductAccountingParams.FEES_RECEIVABLE.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.FEES_RECEIVABLE.getValue()).value(receivableFeeAccountId)
                    .notNull().integerGreaterThanZero();

            final Long receivablePenaltyAccountId = this.fromApiJsonHelper
                    .extractLongNamed(LoanProductAccountingParams.PENALTIES_RECEIVABLE.getValue(), element);
            baseDataValidator.reset().parameter(LoanProductAccountingParams.PENALTIES_RECEIVABLE.getValue())
                    .value(receivablePenaltyAccountId).notNull().integerGreaterThanZero();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForSavingsProductCreate(final String json, DepositAccountType accountType) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(SAVINGS_PRODUCT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        // accounting related data validation
        final Integer accountingRuleType = this.fromApiJsonHelper.extractIntegerNamed(accountingRuleParamName, element,
                Locale.getDefault());
        baseDataValidator.reset().parameter(accountingRuleParamName).value(accountingRuleType).notNull().inMinMaxRange(1, 3);

        if (isCashBasedAccounting(accountingRuleType)) {

            final Long savingsControlAccountId = this.fromApiJsonHelper
                    .extractLongNamed(SavingProductAccountingParams.SAVINGS_CONTROL.getValue(), element);
            baseDataValidator.reset().parameter(SavingProductAccountingParams.SAVINGS_CONTROL.getValue()).value(savingsControlAccountId)
                    .notNull().integerGreaterThanZero();

            final Long savingsReferenceAccountId = this.fromApiJsonHelper
                    .extractLongNamed(SavingProductAccountingParams.SAVINGS_REFERENCE.getValue(), element);
            baseDataValidator.reset().parameter(SavingProductAccountingParams.SAVINGS_REFERENCE.getValue()).value(savingsReferenceAccountId)
                    .notNull().integerGreaterThanZero();

            final Long transfersInSuspenseAccountId = this.fromApiJsonHelper
                    .extractLongNamed(SavingProductAccountingParams.TRANSFERS_SUSPENSE.getValue(), element);
            baseDataValidator.reset().parameter(SavingProductAccountingParams.TRANSFERS_SUSPENSE.getValue())
                    .value(transfersInSuspenseAccountId).notNull().integerGreaterThanZero();

            final Long interestOnSavingsAccountId = this.fromApiJsonHelper
                    .extractLongNamed(SavingProductAccountingParams.INTEREST_ON_SAVINGS.getValue(), element);
            baseDataValidator.reset().parameter(SavingProductAccountingParams.INTEREST_ON_SAVINGS.getValue())
                    .value(interestOnSavingsAccountId).notNull().integerGreaterThanZero();

            final Long incomeFromFeeId = this.fromApiJsonHelper.extractLongNamed(SavingProductAccountingParams.INCOME_FROM_FEES.getValue(),
                    element);
            baseDataValidator.reset().parameter(SavingProductAccountingParams.INCOME_FROM_FEES.getValue()).value(incomeFromFeeId).notNull()
                    .integerGreaterThanZero();

            final Long incomeFromPenaltyId = this.fromApiJsonHelper
                    .extractLongNamed(SavingProductAccountingParams.INCOME_FROM_PENALTIES.getValue(), element);
            baseDataValidator.reset().parameter(SavingProductAccountingParams.INCOME_FROM_PENALTIES.getValue()).value(incomeFromPenaltyId)
                    .notNull().integerGreaterThanZero();

            final Boolean isDormancyTrackingActive = this.fromApiJsonHelper.extractBooleanNamed(isDormancyTrackingActiveParamName, element);
            if (null != isDormancyTrackingActive && isDormancyTrackingActive) {
                final Long escheatLiabilityId = this.fromApiJsonHelper
                        .extractLongNamed(SavingProductAccountingParams.ESCHEAT_LIABILITY.getValue(), element);
                baseDataValidator.reset().parameter(SavingProductAccountingParams.ESCHEAT_LIABILITY.getValue()).value(escheatLiabilityId)
                        .notNull().integerGreaterThanZero();
            }

            if (!accountType.equals(DepositAccountType.RECURRING_DEPOSIT) && !accountType.equals(DepositAccountType.FIXED_DEPOSIT)) {
                final Long overdraftAccount = this.fromApiJsonHelper
                        .extractLongNamed(SavingProductAccountingParams.OVERDRAFT_PORTFOLIO_CONTROL.getValue(), element);
                baseDataValidator.reset().parameter(SavingProductAccountingParams.OVERDRAFT_PORTFOLIO_CONTROL.getValue())
                        .value(overdraftAccount).notNull().integerGreaterThanZero();

                final Long incomeFromInterest = this.fromApiJsonHelper
                        .extractLongNamed(SavingProductAccountingParams.INCOME_FROM_INTEREST.getValue(), element);
                baseDataValidator.reset().parameter(SavingProductAccountingParams.INCOME_FROM_INTEREST.getValue()).value(incomeFromInterest)
                        .notNull().integerGreaterThanZero();

                final Long writtenOff = this.fromApiJsonHelper.extractLongNamed(SavingProductAccountingParams.LOSSES_WRITTEN_OFF.getValue(),
                        element);
                baseDataValidator.reset().parameter(SavingProductAccountingParams.LOSSES_WRITTEN_OFF.getValue()).value(writtenOff).notNull()
                        .integerGreaterThanZero();
            }

        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForShareProductCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ShareProductApiConstants.SHARE_PRODUCT_RESOURCE_NAME);

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        // accounting related data validation
        final Integer accountingRuleType = this.fromApiJsonHelper.extractIntegerNamed(accountingRuleParamName, element,
                Locale.getDefault());
        baseDataValidator.reset().parameter(accountingRuleParamName).value(accountingRuleType).notNull().inMinMaxRange(1, 3);

        if (isCashBasedAccounting(accountingRuleType)) {

            final Long shareReferenceId = this.fromApiJsonHelper.extractLongNamed(SharesProductAccountingParams.SHARES_REFERENCE.getValue(),
                    element);
            baseDataValidator.reset().parameter(SharesProductAccountingParams.SHARES_REFERENCE.getValue()).value(shareReferenceId).notNull()
                    .integerGreaterThanZero();

            final Long shareSuspenseId = this.fromApiJsonHelper.extractLongNamed(SharesProductAccountingParams.SHARES_SUSPENSE.getValue(),
                    element);
            baseDataValidator.reset().parameter(SharesProductAccountingParams.SHARES_SUSPENSE.getValue()).value(shareSuspenseId).notNull()
                    .integerGreaterThanZero();

            final Long incomeFromFeeAccountId = this.fromApiJsonHelper
                    .extractLongNamed(SharesProductAccountingParams.INCOME_FROM_FEES.getValue(), element);
            baseDataValidator.reset().parameter(SharesProductAccountingParams.INCOME_FROM_FEES.getValue()).value(incomeFromFeeAccountId)
                    .notNull().integerGreaterThanZero();

            final Long shareEquityId = this.fromApiJsonHelper.extractLongNamed(SharesProductAccountingParams.SHARES_EQUITY.getValue(),
                    element);
            baseDataValidator.reset().parameter(SharesProductAccountingParams.SHARES_EQUITY.getValue()).value(shareEquityId).notNull()
                    .integerGreaterThanZero();

        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private boolean isCashBasedAccounting(final Integer accountingRuleType) {
        return AccountingRuleType.CASH_BASED.getValue().equals(accountingRuleType);
    }

    private boolean isAccrualBasedAccounting(final Integer accountingRuleType) {
        return AccountingRuleType.ACCRUAL_PERIODIC.getValue().equals(accountingRuleType)
                || AccountingRuleType.ACCRUAL_UPFRONT.getValue().equals(accountingRuleType);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }
}
