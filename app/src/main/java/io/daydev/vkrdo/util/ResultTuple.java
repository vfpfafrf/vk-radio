package io.daydev.vkrdo.util;

/**
 * Created by dmitry on 06.03.15.
 */
public class ResultTuple<V> extends Tuple<V, String> {

    public static <V> ResultTuple<V> error (String error){
        return new ResultTuple<>(null, error);
    }

    public static <V> ResultTuple<V> success(V result){
        return new ResultTuple<>(result, null);
    }

    public ResultTuple(V result, String error) {
        super(result, error);
    }

    public V getResult(){
        return getFirst();
    }

    public String getError(){
        return getSecond();
    }

    public boolean hasError(){
        return getSecond() != null;
    }

    public boolean hasResult(){
        return getFirst() != null;
    }
}
