-- Postgres round() rounds ties away from zero; interest is specified as half-even.
CREATE FUNCTION round_half_even(value NUMERIC) RETURNS NUMERIC AS $$
    SELECT CASE
        WHEN value * 100 - floor(value * 100) = 0.5
            THEN (floor(value * 100) + mod(floor(value * 100), 2)) / 100
        ELSE round(value, 2)
    END
$$ LANGUAGE sql IMMUTABLE;
