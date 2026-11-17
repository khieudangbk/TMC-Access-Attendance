package com.tmc.api;

import java.util.List;

public record PageData<T>(List<T> items, int limit, int offset, long total) {
}
