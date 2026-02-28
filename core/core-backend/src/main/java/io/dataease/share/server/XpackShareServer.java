package io.dataease.share.server;

import io.dataease.api.visualization.request.VisualizationWorkbranchQueryRequest;
import io.dataease.api.xpack.share.XpackShareApi;
import io.dataease.api.xpack.share.request.*;
import io.dataease.api.xpack.share.vo.XpackShareGridVO;
import io.dataease.api.xpack.share.vo.XpackShareProxyVO;
import io.dataease.api.xpack.share.vo.XpackShareVO;
import io.dataease.auth.DePermit;
import io.dataease.exception.DEException;
import io.dataease.result.ResultCode;
import io.dataease.utils.BeanUtils;
import io.dataease.share.dao.auto.entity.XpackShare;
import io.dataease.share.manage.XpackShareManage;
import io.dataease.utils.AuthUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequestMapping("/share")
@RestController
public class XpackShareServer implements XpackShareApi {

    @Resource(name = "xpackShareManage")
    private XpackShareManage xpackShareManage;
    @Override
    @DePermit({"#p0 + ':share'"})
    public boolean status(Long resourceId) {
        return ObjectUtils.isNotEmpty(xpackShareManage.queryByResource(resourceId));
    }

    @Override
    @DePermit({"#p0 + ':share'"})
    public void switcher(Long resourceId) {
        xpackShareManage.switcher(resourceId);
    }

    @Override
    @DePermit({"#p0.resourceId + ':share'"})
    public void editExp(XpackShareExpRequest request) {
        xpackShareManage.editExp(request.getResourceId(), request.getExp());
    }

    @Override
    @DePermit({"#p0.resourceId + ':share'"})
    public void editPwd(XpackSharePwdRequest request) {
        xpackShareManage.editPwd(request.getResourceId(), request.getPwd(), request.getAutoPwd());
    }

    @Override
    @DePermit({"#p0 + ':share'"})
    public XpackShareVO detail(Long resourceId) {
        XpackShare xpackShare = xpackShareManage.queryByResource(resourceId);
        if (ObjectUtils.isEmpty(xpackShare)) return null;
        return BeanUtils.copyBean(new XpackShareVO(), xpackShare);
    }

    @Override
    public List<XpackShareGridVO> query(VisualizationWorkbranchQueryRequest request) {
        return xpackShareManage.query(1, 20, request).getRecords();
    }

    @Override
    public XpackShareProxyVO proxyInfo(XpackShareProxyRequest request) {
        return xpackShareManage.proxyInfo(request);
    }

    @Override
    public boolean validatePwd(XpackSharePwdValidator validator) {
        return xpackShareManage.validatePwd(validator);
    }

    @Override
    public Map<String, String> queryRelationByUserId(Long uid) {
        var user = AuthUtils.getUser();
        if (user == null) {
            DEException.throwException(ResultCode.USER_NOT_LOGGED_IN.code(), ResultCode.USER_NOT_LOGGED_IN.message());
        }
        if (!AuthUtils.isSysAdmin() && !Objects.equals(uid, user.getUserId())) {
            DEException.throwException(ResultCode.INTERFACE_FORBID_VISIT.code(), ResultCode.INTERFACE_FORBID_VISIT.message());
        }
        return xpackShareManage.queryRelationByUserId(uid);
    }

    @Override
    @DePermit({"#p0.resourceId + ':share'"})
    public String editUuid(XpackShareUuidEditor editor) {
        return xpackShareManage.editUuid(editor);
    }
}
