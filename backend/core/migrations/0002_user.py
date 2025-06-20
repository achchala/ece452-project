"""Migration for User model."""

from django.db import migrations, models


class Migration(migrations.Migration):
    """User model migration."""

    dependencies = [
        ("core", "0001_initial"),
    ]

    operations = [
        migrations.CreateModel(
            name="User",
            fields=[
                (
                    "id",
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name="ID",
                    ),
                ),
                ("email", models.EmailField(max_length=254, unique=True)),
                ("date_joined", models.DateTimeField(auto_now_add=True)),
                ("firebase_id", models.CharField(max_length=255, unique=True)),
            ],
            options={
                "db_table": "users",
                "ordering": ["-date_joined"],
            },
        ),
    ]
