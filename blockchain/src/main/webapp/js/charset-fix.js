/**
 * 字符集修复工具 - 确保所有页面正确处理中文字符及编码
 */

// 确保页面字符集正确设置为UTF-8
document.addEventListener('DOMContentLoaded', function() {
    // 检查meta字符集标签
    const metaCharset = document.querySelector('meta[charset]');
    if (metaCharset && metaCharset.getAttribute('charset').toLowerCase() !== 'utf-8') {
        metaCharset.setAttribute('charset', 'UTF-8');
        console.info('字符集已修正为UTF-8');
    }
    
    // 对URL参数进行正确编码处理
    function fixUrlEncoding() {
        const links = document.querySelectorAll('a[href]');
        links.forEach(link => {
            const href = link.getAttribute('href');
            if (href.includes('?') && href.includes('=')) {
                try {
                    // 检查URL是否包含中文参数，如果有则确保正确编码
                    const url = new URL(href, window.location.origin);
                    const params = url.searchParams;
                    let needsUpdate = false;
                    
                    // 检查每个参数
                    for (const [key, value] of params.entries()) {
                        if (/[\u4e00-\u9fa5]/.test(value) && value !== decodeURIComponent(encodeURIComponent(value))) {
                            params.set(key, decodeURIComponent(encodeURIComponent(value)));
                            needsUpdate = true;
                        }
                    }
                    
                    if (needsUpdate) {
                        link.setAttribute('href', url.toString());
                    }
                } catch (e) {
                    console.warn('URL编码修复失败:', e);
                }
            }
        });
    }
    
    fixUrlEncoding();
    
    // 修复登录状态保持问题
    function fixAuthPersistence() {
        // 在页面加载或刷新时，确保检查一下登录状态
        if (typeof AuthHelper !== 'undefined' && AuthHelper.isAuthenticated()) {
            // 尝试验证登录状态
            AuthHelper.verifyAuthStatus()
                .then(isValid => {
                    if (isValid) {
                        console.info('登录状态已验证');
                        // 刷新一下localStorage的过期时间
                        const currentDid = AuthHelper.getCurrentDid();
                        const authToken = AuthHelper.getAuthToken();
                        if (currentDid && authToken) {
                            localStorage.setItem('blockchain_auth_did', currentDid);
                            localStorage.setItem('blockchain_auth_token', authToken);
                            localStorage.setItem('blockchain_auth_status', 'authenticated');
                        }
                    }
                })
                .catch(err => {
                    console.warn('登录状态验证失败:', err);
                });
        }
    }
    
    // 在文档加载1秒后再次检查登录状态，确保稳定性
    setTimeout(fixAuthPersistence, 1000);
});
