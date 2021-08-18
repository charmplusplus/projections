package projections.gui;

class SparseArray_usage{
    int l=0;
    usage_data[][] data;
    SparseArray_usage(int procCnt){
        l=procCnt;
        data = new usage_data[procCnt][];
    }
    void add_element(int idx, int len){
        data[idx] = new usage_data[len];
    }
    int length(int idx){
        return data[idx].length;
    }
    int length(){
        return l;
    }
    void add_data_point(int idx,int idx2, float f,int i, String s){
        data[idx][idx2] = new usage_data(f, i, s);
    }
    usage_data get(int idx1,int idx2){
        return data[idx1][idx2];
    }
}

class usage_data{
    float dataSource;
    int colorMap;
    String nameMap;
    usage_data(float f,int i, String s){
        dataSource=f;
        colorMap=i;
        nameMap = s;
    }
}
