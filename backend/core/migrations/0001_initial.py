"""Initial migration for FriendRequest model."""

from django.db import migrations, models


class Migration(migrations.Migration):
    """Initial migration."""

    initial = True

    dependencies = []

    operations = [
        migrations.CreateModel(
            name="FriendRequest",
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
                ("from_user", models.CharField(max_length=100)),
                ("to_user", models.CharField(max_length=100)),
                ("request_completed", models.BooleanField(default=False)),
                ("created_at", models.DateTimeField(auto_now_add=True)),
            ],
            options={
                "unique_together": {("from_user", "to_user")},
                "db_table": "friend_requests",
            },
        ),
    ]
