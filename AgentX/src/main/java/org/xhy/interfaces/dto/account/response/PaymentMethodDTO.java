package org.xhy.interfaces.dto.account.response;

import java.util.List;

/** 支付方法DTO */
public class PaymentMethodDTO {

    /** 支付平台代码 */
    private String platformCode;

    /** 支付平台名称 */
    private String platformName;

    /** 是否可用 */
    private boolean available;

    /** 支持的支付类型列表 */
    private List<PaymentTypeDTO> paymentTypes;

    /** 平台描述 */
    private String description;

    /** 平台图标URL */
    private String iconUrl;

    public PaymentMethodDTO() {
    }

    public PaymentMethodDTO(String platformCode, String platformName, boolean available,
            List<PaymentTypeDTO> paymentTypes) {
        this.platformCode = platformCode;
        this.platformName = platformName;
        this.available = available;
        this.paymentTypes = paymentTypes;
    }

    public String getPlatformCode() {
        return platformCode;
    }

    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public List<PaymentTypeDTO> getPaymentTypes() {
        return paymentTypes;
    }

    public void setPaymentTypes(List<PaymentTypeDTO> paymentTypes) {
        this.paymentTypes = paymentTypes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    /** 支付类型DTO */
    public static class PaymentTypeDTO {

        /** 支付类型代码 */
        private String typeCode;

        /** 支付类型名称 */
        private String typeName;

        /** 是否需要跳转 */
        private boolean requireRedirect;

        /** 类型描述 */
        private String description;

        public PaymentTypeDTO() {
        }

        public PaymentTypeDTO(String typeCode, String typeName, boolean requireRedirect) {
            this.typeCode = typeCode;
            this.typeName = typeName;
            this.requireRedirect = requireRedirect;
        }

        public String getTypeCode() {
            return typeCode;
        }

        public void setTypeCode(String typeCode) {
            this.typeCode = typeCode;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public boolean isRequireRedirect() {
            return requireRedirect;
        }

        public void setRequireRedirect(boolean requireRedirect) {
            this.requireRedirect = requireRedirect;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return "PaymentTypeDTO{" + "typeCode='" + typeCode + '\'' + ", typeName='" + typeName + '\''
                    + ", requireRedirect=" + requireRedirect + ", description='" + description + '\'' + '}';
        }
    }

    @Override
    public String toString() {
        return "PaymentMethodDTO{" + "platformCode='" + platformCode + '\'' + ", platformName='" + platformName + '\''
                + ", available=" + available + ", paymentTypes=" + paymentTypes + ", description='" + description + '\''
                + ", iconUrl='" + iconUrl + '\'' + '}';
    }
}