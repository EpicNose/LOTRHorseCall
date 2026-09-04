package com.epicnose.lotrcallablehorse.lotr.common.mount;

import com.epicnose.lotrcallablehorse.lotr.common.mount.adapters.GenericMountAdapter;
import com.epicnose.lotrcallablehorse.lotr.common.mount.adapters.HorseMountAdapter;
import com.epicnose.lotrcallablehorse.lotr.common.mount.adapters.SpiderMountAdapter;
import com.epicnose.lotrcallablehorse.lotr.common.mount.adapters.WargMountAdapter;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 坐骑适配器注册中心与派发工厂。
 */
public final class MountAdapterRegistry {
    private static final List<IMountAdapter> ADAPTERS = new ArrayList<IMountAdapter>();
    private static final IMountAdapter FALLBACK = new GenericMountAdapter();

    static {
        // 按匹配优先级注册：马系优先、座狼次之、蜘蛛次之，最后通用兜底
        ADAPTERS.add(new HorseMountAdapter());
        ADAPTERS.add(new WargMountAdapter());
        ADAPTERS.add(new SpiderMountAdapter());
    }

    private MountAdapterRegistry() {
    }

    public static IMountAdapter getAdapter(Entity entity) {
        if (entity == null) {
            return FALLBACK;
        }
        for (IMountAdapter adapter : ADAPTERS) {
            if (adapter.matches(entity)) {
                return adapter;
            }
        }
        return FALLBACK;
    }

    public static IMountAdapter getFallback() {
        return FALLBACK;
    }
}
