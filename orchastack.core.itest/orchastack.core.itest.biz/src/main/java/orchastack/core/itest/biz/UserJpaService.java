package orchastack.core.itest.biz;

import orchastack.core.itest.entity.CloudUser1;

public interface UserJpaService {

	public abstract CloudUser1 saveWithTx(CloudUser1 user) throws Exception;

}