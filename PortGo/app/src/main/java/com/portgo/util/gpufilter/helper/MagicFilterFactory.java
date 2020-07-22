package com.portgo.util.gpufilter.helper;


import android.content.Context;

import com.portgo.util.gpufilter.basefilter.GPUImageFilter;
import com.portgo.util.gpufilter.filter.MagicAntiqueFilter;
import com.portgo.util.gpufilter.filter.MagicBrannanFilter;
import com.portgo.util.gpufilter.filter.MagicCoolFilter;
import com.portgo.util.gpufilter.filter.MagicFreudFilter;
import com.portgo.util.gpufilter.filter.MagicHefeFilter;
import com.portgo.util.gpufilter.filter.MagicHudsonFilter;
import com.portgo.util.gpufilter.filter.MagicInkwellFilter;
import com.portgo.util.gpufilter.filter.MagicN1977Filter;
import com.portgo.util.gpufilter.filter.MagicNashvilleFilter;
import com.portgo.util.gpufilter.basefilter.GPUImageFilter;

public class MagicFilterFactory {

    private static MagicFilterType filterType = MagicFilterType.NONE;

    public static GPUImageFilter initFilters(Context context,MagicFilterType type) {
        if (type == null) {
            return null;
        }
        filterType = type;
        switch (type) {
            case ANTIQUE:
                return new MagicAntiqueFilter(context);
            case BRANNAN:
                return new MagicBrannanFilter(context);
            case FREUD:
                return new MagicFreudFilter(context);
            case HEFE:
                return new MagicHefeFilter(context);
            case HUDSON:
                return new MagicHudsonFilter(context);
            case INKWELL:
                return new MagicInkwellFilter(context);
            case N1977:
                return new MagicN1977Filter(context);
            case NASHVILLE:
                return new MagicNashvilleFilter(context);
            case COOL:
                return new MagicCoolFilter(context);
            case WARM:
                return new MagicWarmFilter(context);
            default:
                return null;
        }
    }

    public MagicFilterType getCurrentFilterType() {
        return filterType;
    }

    private static class MagicWarmFilter extends GPUImageFilter {
        MagicWarmFilter(Context context){
            super(context);
        }
    }
}
