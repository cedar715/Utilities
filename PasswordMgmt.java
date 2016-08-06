   public String obfuscatePassword(String password) {
        int len = password.length();
        return len <= 4 ? password:password.substring(0, 2) + StringUtils.repeat("*", len - 4) + password.substring(len - 2, len);
    }
