package orchastack.core.itest.biz;

import orchastack.core.itest.entity.CloudUser1;

public interface UserCompoundService {

	public abstract CloudUser1 saveOrUpdate(CloudUser1 user) throws Exception;

}